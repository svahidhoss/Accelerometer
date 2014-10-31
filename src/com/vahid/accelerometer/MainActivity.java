package com.vahid.accelerometer;

import java.util.Calendar;

import com.vahid.accelerometer.bluetooth.BluetoothDevicesActivity;
import com.vahid.accelerometer.bluetooth.ConnectThread;
import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.util.MathUtil;
import com.vahid.accelerometer.util.Constants;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// used for exiting on pressing back double
	private boolean doubleBackToExitIsPressedOnce = false;

	/**** Bluethooth related fields ****/
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private BroadcastReceiver mReceiver;

	private ConnectThread mConnectThread = null;
	private ConnectedThread mConnectedThread = null;
	private String mDeviceName = "";

	private static int mCurrentBTState = Constants.STATE_DISCONNECTED;

	/**** Defining view fields ****/
	// 1.Initial views
	private Button btnConnectBT, btnCheck, btnRunBarsAct;
	private TextView tvState;
	private MenuItem miSearchOption;

	// Sensor Values: it's important to initialize them.
	private float[] acceleromterValues = new float[] { 0, 0, 0 };

	/**** Alex values ****/
	// --- Filters ---
	boolean breakOn = false; // on when is more than one minimum defined
								// (Constant.precision)
	boolean breakReal = false; // when the braking is more long than
								// (Constant.marginMilliseconds)
	Calendar breakInitializedTime = null;

	// ****calculate angles average
	boolean noise = false;
	boolean onAngles = false;
	float sum_angles1 = 0f;
	float sum_angles1_aux = 0f;
	float sum_angles2 = 0f;
	float sum_angles2_aux = 0f;
	int n = 0;
	int n_aux = 0;
	float[] orientationValuesEarlier = new float[] { 0, 0, 0 };

	// *****end*angles average

	// -end--filters--

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// keeps the screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// include setContentView, listener for the button, state, etc
		initViews();
	}

	@Override
	protected void onStop() {
		super.onStop();
		finilizeAll();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		// set the miSearchOption as the first item of the menu.
		miSearchOption = menu.getItem(1);
		return true;
	}

	/**
	 * 1st Important function of this activity. Initializes the views of this
	 * activity when no device is connected.
	 */
	private void initViews() {
		setContentView(R.layout.activity_main);

		btnConnectBT = (Button) findViewById(R.id.btnConnectBT);
		btnRunBarsAct = (Button) findViewById(R.id.btnRunBarsAct);

		tvState = (TextView) findViewById(R.id.textViewNotConnected);

		setStatus(R.string.title_not_connected);

		// if we need to use BT
		if (Constants.BT_MODULE_EXISTS && mBluetoothAdapter == null) {
			noBluetoothDetected();
		} else {
			btnConnectBT.setVisibility(View.VISIBLE);

			if (!mBluetoothAdapter.isEnabled()) {
				tvState.setText("Bluetooth is turned OFF");
				btnConnectBT.setText(" Turn ON Bluetooth ");
			} else if (mBluetoothAdapter.isEnabled()) {
				tvState.setText("Not Connected");
				btnConnectBT.setText(" Connect Now ");
			}
		}

	}

	/**
	 * Manages all the clicks on the buttons of this Activity.
	 * 
	 * @param view
	 */
	public void onButtonClicked(View view) {
		switch (view.getId()) {
		case R.id.btnConnectBT:
			// open the file if set true, otherwise close it.
			if (Constants.BT_MODULE_EXISTS)
				initializeBluetooth();
			else {
				runConnectedDebugActivity();
				return;
			}
			break;
		case R.id.btnRunBarsAct:
			// open the file if set true, otherwise close it.
			runConnectedBarsActivity();
			break;
		}
	}

	/**
	 * Method that checks if the device supports Bluetooth, asks for enabling it
	 * if not already, find devices with BluetoothDevices.class and registers
	 * the required Receivers.
	 */
	private void initializeBluetooth() {
		if (mBluetoothAdapter == null) {
			noBluetoothDetected();
			return;
		} else if (!mBluetoothAdapter.isEnabled()) {
			enableBluetoothDialog();
		} else {
			runBluetoothDevicesActivity();
		}

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

					if (intent.getIntExtra(
							BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned off", Toast.LENGTH_SHORT)
								.show();
						initViews();
					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned ON", Toast.LENGTH_SHORT)
								.show();
						initViews();
					}

				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED
						.equals(action)) {
					// check this
					// we receive this information also from the connectThread
					// and the connectedThread.

					// Toast.makeText(getApplicationContext(),
					// "Conexion was lost - broadcast",
					// Toast.LENGTH_SHORT).show();
					// notConnected();
					// return;
				} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
					// connected();
				}

			}
		};
		IntentFilter filter;
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED); //
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		registerReceiver(mReceiver, filter);

	}

	/**
	 * Starts Bluetooth Devices Activity so that the user can look for and
	 * select the device (its MAC address) so that it can connect to it.
	 */
	private void runBluetoothDevicesActivity() {
		// If requestCode >= 0, it will be returned in onActivityResult() when
		// the activity exits.
		startActivityForResult(
				new Intent(this, BluetoothDevicesActivity.class),
				Constants.REQUEST_CONNECT_DEVICE);
	}

	/**
	 * Establishes the Bluetooth connection with the passed device address.
	 * 
	 * @param address
	 */
	private void connectBluetoothDevice(String address) {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		mConnectThread = new ConnectThread(device, mHandler);
		mConnectThread.start();

		mDeviceName = device.getName();
		// the receiver (mReceiver) is waiting for the signal of
		// "device connected" to use the connection, and call connected().
		// connected() initialize also the ConnectedThread instance (- connected
		// = new ConnectedThread(BleutoothSocket) -)
	}

	/**
	 * Method that stops everything.
	 */
	private void finilizeAll() {
		// disconnect the thread first
		if (mConnectedThread != null) {
			mConnectThread.cancel();
		}

		// unregister the receivers
		if (mReceiver != null) {
			this.unregisterReceiver(mReceiver);
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_option:
			Intent intentSettings = new Intent(this, SettingsActivity.class);
			startActivityForResult(intentSettings,
					Constants.REQUEST_SETTINGS_CHANGE);
			return true;

		case R.id.search_option:
			if (mCurrentBTState == Constants.STATE_DISCONNECTED) {
				initializeBluetooth();
				return true;
			} else {
				if (mConnectedThread != null) {
					mConnectedThread.cancel();
				}
				return true;
			}

		case R.id.about_option:
			Toast.makeText(this, "Car Brake Detector Demo\nBy Vahid",
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * This handler is used to enable communication with the threads.
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mCurrentBTState = msg.what;

			switch (msg.what) {
			case Constants.STATE_CONNECTED:
				runConnected();
				setStatus(getString(R.string.title_connected) + mDeviceName);
				// change the connect icon on the activity.
				if (miSearchOption != null) {
					miSearchOption.setIcon(R.drawable.menu_disconnect_icon);
					miSearchOption.setTitle(R.string.disconnect);
				}
				break;
			case Constants.STATE_CONNECTING:
				TextView tvState = (TextView) findViewById(R.id.textViewNotConnected);
				tvState.setText(R.string.title_connecting);
				setStatus(R.string.title_connecting);
				break;
			case Constants.STATE_DISCONNECTED:
				Toast.makeText(getApplicationContext(),
						getString(R.string.msgUnableToConnect),
						Toast.LENGTH_SHORT).show();
				initViews();
				miSearchOption.setIcon(R.drawable.menu_connect_icon);
				miSearchOption.setTitle(R.string.connect);
				break;

			default:
				break;
			}
		}

	};

	/**
	 * Dialog that is displayed when no bluetooth is found on the device. The
	 * app then closes.
	 */
	private void noBluetoothDetected() {
		btnConnectBT.setVisibility(View.GONE);
		tvState.setText("Device does not support Bluetooth");
		ImageView ivError = (ImageView) findViewById(R.id.imageViewWrong);
		ivError.setVisibility(View.VISIBLE);
	}

	/**
	 * Dialog that asks from the user to enable the bluetooth.
	 */
	private void enableBluetoothDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				MainActivity.this);
		// Setting Dialog Title
		alertDialog.setTitle("Info!");
		// Setting Dialog Message
		alertDialog
				.setMessage("Bluetooth must be enabled on the phone!\n\nDo you wish to continue?");
		// Setting Icon to Dialog
		alertDialog.setIcon(R.drawable.ic_warning);
		// Setting OK Button
		alertDialog.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// enable bluetooth
						Intent enableBtIntent = new Intent(
								BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableBtIntent,
								Constants.REQUEST_ENABLE_BT);
					}
				});
		alertDialog.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(MainActivity.this, R.string.bt_required,
								Toast.LENGTH_SHORT).show();
					}
				});
		alertDialog.create();
		alertDialog.show();
	}

	/**
	 * Using the following function of "clicking TWICE the back button to exit
	 * app" has been implemented.
	 */
	@Override
	public void onBackPressed() {
		if (doubleBackToExitIsPressedOnce) {
			super.onBackPressed();
			return;
		}
		this.doubleBackToExitIsPressedOnce = true;
		Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT)
				.show();
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				doubleBackToExitIsPressedOnce = false;
			}
		}, 2000);
	}

	/**
	 * Method used for setting up status title of the action bar.
	 * 
	 * @param subTitle
	 */
	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	/**
	 * Method used for setting up status title of the action bar.
	 * 
	 * @param resourceId
	 */
	private final void setStatus(int resourceId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resourceId);
	}

	/**
	 * Runs the connected activity. I used debug because it's showing various
	 * Accel. values.
	 */
	private void runConnectedDebugActivity() {
		Intent intent = new Intent(this, ConnectedDebugActivity.class);
		startActivity(intent);
	}

	/**
	 * Runs the connected bar activity.
	 */
	private void runConnectedBarsActivity() {
		Intent intent = new Intent(this, ConnectedBarsActivity.class);
		startActivity(intent);
	}

	private void writeToBluetoothDevice(double magnitude) {
		// ****writing also the module when brake is real.
		double moduleReal;
		if (breakReal) {
			moduleReal = magnitude;
		} else {
			moduleReal = 0;
		}

		// ---with the idea of write this data and send by bluetooth,
		// first it's necessary to covert them to byte...
		byte[] x = MathUtil.doubleToByteArray(acceleromterValues[0]);
		byte[] y = MathUtil.doubleToByteArray(acceleromterValues[1]);
		byte[] z = MathUtil.doubleToByteArray(acceleromterValues[2]);
		byte[] mod_byte = MathUtil.doubleToByteArray(magnitude);
		byte[] xyz_and_Mod = new byte[8 * 4];

		xyz_and_Mod = MathUtil.concatenateBytes(
				MathUtil.concatenateBytes(MathUtil.concatenateBytes(x, y), z),
				mod_byte);
		// ---

		byte[] moduleRealByte = MathUtil.doubleToByteArray(moduleReal);
		byte[] all = new byte[8 * 4 + 8];
		all = MathUtil.concatenateBytes(xyz_and_Mod, moduleRealByte);

		mConnectedThread.write(all);
		// ********write angles
		/*
		 * byte[] az = mmath.toByteArray(orientationValues[0]); byte[] pitch =
		 * mmath.toByteArray(orientationValues[1]); byte[] roll =
		 * mmath.toByteArray(orientationValues[2]); byte[] anglesByte =
		 * mmath.concatenateBytes(mmath.concatenateBytes(az, pitch), roll);
		 * 
		 * connected.write(mmath.concatenateBytes(anglesByte, mod_byte));
		 */
		// /****end write angles

		// *****end***writing also the module when brake is real.

	}

	private void runConnected() {
		// TODO Auto-generated method stub

		Toast.makeText(getApplicationContext(),
				"Connected with " + mDeviceName + "!", Toast.LENGTH_SHORT)
				.show();

		setContentView(R.layout.activity_main);

		// *******
		BluetoothSocket mSocket = mConnectThread.getBluetoothSocket();
		mConnectedThread = new ConnectedThread(mSocket, mHandler); // this
																	// instance
																	// of
																	// ConnectedThread
																	// is the
																	// one that
																	// we are
																	// going to
																	// use to
																	// write()
		// we don't need to start the Thread, because we are going to write, not
		// to read() [write is not a blocking method]
		// *******

		/**
		 * we are ready to use the sensor and send the information of the
		 * brakings, so...
		 */
		// ******SENSORS***********
		// initializeSensors();
		// ******END SENSORS*******

		// ******example temp**********
		/*
		 * Button b = (Button) findViewById(R.id.button1);
		 * 
		 * b.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) {
		 * mConnectedThread.write(MathUtil.toByteArray(60));
		 * 
		 * 
		 * 
		 * } });
		 */

		// ********end**example******

	}

}
