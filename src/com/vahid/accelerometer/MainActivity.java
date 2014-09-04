package com.vahid.accelerometer;

import java.util.Calendar;

import com.vahid.accelerometer.bluetooth.BluetoothDevicesActivity;
import com.vahid.accelerometer.bluetooth.ConnectThread;
import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.util.AlexMath;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.acceleromter.location.MyLocationListener;

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
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_CONNECT_DEVICE = 2;

	// used for exiting on pressing back double
	private boolean doubleBackToExitIsPressedOnce = false;

	/**** Bluethooth related fields ****/
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private BroadcastReceiver mReceiver;

	private ConnectThread connectThread = null;
	private ConnectedThread connectedThread = null;
	private String deviceName = "";

	private static int currentState = Constants.STATE_DISCONNECTED;

	/**** Defining view fields ****/
	private Button btnConnect, buttonCheck;
	private TextView tvState, tvLASCapturedState;
	private MenuItem miSearchOption;
	// Connected views
	private SeekBar xAxisSeekBar, yAxisSeekBar, zAxisSeekBar, finalSeekBar;
	private TextView tvXAxisValue, tvYAxisValue, tvZAxisValue, tvFinalValue;
	/* the Spinner component for delay rate */
	private Spinner delayRateChooser;
	private CheckBox checkBoxSaveToFile;

	// save to file view fields
	// private boolean saveToFileChecked = false;
	private CsvFileWriter csvFile;
	private CsvFileWriter csvLocationFile;


	/**** Location Related fields ****/
	private MyLocationListener myLocationListener;
	private LocationManager locationManager;

	/**** Sensor related Fields ****/
	// private SensorManager mSensorManager;
	private AccelerationEventListener accelerationEventListener;

	// Sensor Accelerometer Rates
	private static final int delayRates[] = {
			SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI,
			SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST };
	private static final String delayRatesDescription[] = {
			"Normal 200,000 ms", "UI 60,000 ms", "Game 20,000 ms",
			"Fastest 0 ms" };
	private int currentDelayRate = SensorManager.SENSOR_DELAY_NORMAL;

	// Sensor Values: it's important to initialize them.
	private float[] acceleromterValues = new float[] { 0, 0, 0 };
	private float[] orientationValues = new float[] { 0, 0, 0 };
	private float[] linearAccelerationValues = new float[] { 0, 0, 0 };
	private float[] magneticValues;
	private float[] gravityValues;

	private double linearAccelerationMagnitude;

	// Rotation Matrix Calculation
	private float[] trueAcceleration = new float[4];
	private float[] inclinationMatrix = new float[16];
	private float[] rotationMatrix = new float[16];
	private float[] rotationMatrixInverse = new float[16];

	private double trueAccelerationMagnitude;

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
		initViewsNotConnected();
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
		miSearchOption = menu.getItem(1);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.bt_required, Toast.LENGTH_SHORT)
						.show();
			}
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, "Bluetooth is enabled.",
						Toast.LENGTH_SHORT).show();
				runBluetoothDevicesActivity();
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == RESULT_OK) {
				String passedAddress = data.getExtras().getString(
						BluetoothDevicesActivity.EXTRA_ADDRESS);

				connectBluetoothDevice(passedAddress);
			} else {
				initViewsNotConnected();
			}
			break;

		default:
			break;
		}
	}

	/**
	 * 1st Important function of this activity. Initializes the views of this
	 * activity when no device is connected.
	 */
	private void initViewsNotConnected() {
		setContentView(R.layout.activity_main_not_connected);

		btnConnect = (Button) findViewById(R.id.buttonConnect);
		tvState = (TextView) findViewById(R.id.textViewNotConnected);

		btnConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Constants.BT_MODULE_EXISTS)
					initializeBluetooth();
				else {
					initViewsConnectedLinearAcceleration();
					// initViewsConnected();
					// no connection needed, end method here
					return;
				}
			}
		});

		setStatus(R.string.title_not_connected);

		if (mBluetoothAdapter == null) {
			noBluetoothDetected();
		} else {
			btnConnect.setVisibility(View.VISIBLE);

			if (!mBluetoothAdapter.isEnabled()) {
				tvState.setText("Bluetooth is turned OFF");
				btnConnect.setText(" Turn ON Bluetooth ");
			} else if (mBluetoothAdapter.isEnabled()) {
				tvState.setText("Not Connected");
				btnConnect.setText(" Connect Now ");
			}
		}

		if (accelerationEventListener != null) {
			accelerationEventListener.unregisterSensors();
		}
	}

	/**
	 * 2nd Important function of this activity. Initializes the views of this
	 * activity when a device gets connected.
	 */
	private void initViewsConnected() {

		Toast.makeText(getApplicationContext(),
				getString(R.string.title_connected) + deviceName,
				Toast.LENGTH_SHORT).show();
		setContentView(R.layout.activity_main_connected);

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].
		if (Constants.BT_MODULE_EXISTS) {
			BluetoothSocket mSocket = connectThread.getBluetoothSocket();
			connectedThread = new ConnectedThread(mSocket, mHandler);
		}

		// we are ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerationEventListener = new AccelerationEventListener(mHandler);
		accelerationEventListener.initializeSensors(mSensorManager);
		accelerationEventListener.registerSensors(currentDelayRate);

		// TODO what is this really doing?
		buttonCheck = (Button) findViewById(R.id.buttonCheck);
		buttonCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Constants.BT_MODULE_EXISTS)
					connectedThread.write(AlexMath.toByteArray(60));
				else {

				}
			}
		});

	}

	/**
	 * 2nd Important function of this activity. Initializes the views of this
	 * activity when a device gets connected.
	 */
	private void initViewsConnectedLinearAcceleration() {
		Toast.makeText(getApplicationContext(),
				getString(R.string.title_connected) + deviceName,
				Toast.LENGTH_SHORT).show();

		setContentView(R.layout.activity_main_las);

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].

		if (Constants.BT_MODULE_EXISTS) {
			BluetoothSocket mSocket = connectThread.getBluetoothSocket();
			connectedThread = new ConnectedThread(mSocket, mHandler);

		}

		// we are ready
		myLocationListener = new MyLocationListener(getApplicationContext(),
				mHandler);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				10000, 5, myLocationListener);
		// ?
		// provider = myLocationManager.getBestProvider(criteria, false);
		// Location loc = myLocationManager
		// .getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// we are also ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerationEventListener = new AccelerationEventListener(mHandler);
		accelerationEventListener.initializeSensors(mSensorManager);
		accelerationEventListener.registerSensors(currentDelayRate);

		// retrieve all the needed components
		xAxisSeekBar = (SeekBar) findViewById(R.id.xAxisBar);
		yAxisSeekBar = (SeekBar) findViewById(R.id.yAxisBar);
		zAxisSeekBar = (SeekBar) findViewById(R.id.zAxisBar);
		finalSeekBar = (SeekBar) findViewById(R.id.finalBar);

		tvXAxisValue = (TextView) findViewById(R.id.xAxisValue);
		tvYAxisValue = (TextView) findViewById(R.id.yAxisValue);
		tvZAxisValue = (TextView) findViewById(R.id.zAxisValue);
		tvFinalValue = (TextView) findViewById(R.id.finalValue);

		checkBoxSaveToFile = (CheckBox) findViewById(R.id.checkBoxSaveToFile);

		// populate the delay rate spinner
		populateDelayRateSpinner();
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
						initViewsNotConnected();
					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned ON", Toast.LENGTH_SHORT)
								.show();
						initViewsNotConnected();
					}

				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED
						.equals(action)) {// check this //#//
					// we recieve this information also from the connectThread
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
				REQUEST_CONNECT_DEVICE);
	}

	/**
	 * Establishes the Bluetooth connection with the passed device address.
	 * 
	 * @param address
	 */
	private void connectBluetoothDevice(String address) {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		connectThread = new ConnectThread(device, mHandler);
		connectThread.start();

		deviceName = device.getName();
		// the receiver (mReceiver) is waiting for the signal of
		// "device connected" to use the connection, and call connected().
		// connected() initialize also the ConnectedThread instance (- connected
		// = new ConnectedThread(BleutoothSocket) -)
	}

	/**
	 * Method that stops everything.
	 */
	private void finilizeAll() {
		initViewsNotConnected(); // #check, ... I put this here because when the
		// orientation changes, the Activity is destroyed, so
		// the device is disconnecting automatically
		if (accelerationEventListener != null) {
			accelerationEventListener.unregisterSensors();
		}
		// disconnect the thread first
		if (connectedThread != null) {
			connectThread.cancel();
		}

		// unregister the receivers
		if (mReceiver != null) {
			this.unregisterReceiver(mReceiver);
		}

		// close the captured file if not already
		if (csvFile != null) {
			csvFile.closeCaptureFile();
			checkBoxSaveToFile.setText(R.string.checkBoxSaveToFileInitialMsg);
		}
		
		if (csvLocationFile != null) {
			csvLocationFile.closeCaptureFile();
		}

	}

	private void populateDelayRateSpinner() {
		delayRateChooser = (Spinner) findViewById(R.id.delayRateChooser);
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this,
				R.array.delay_rates, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		delayRateChooser.setAdapter(adapter);

		// set the action to perform when an item is selected
		delayRateChooser
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView,
							View selectedItemView, int position, long id) {
						if (currentDelayRate != position) {
							currentDelayRate = position;
							if (accelerationEventListener != null) {
								accelerationEventListener
										.registerSensors(currentDelayRate);
							}
							// show a toast message
							/*
							 * toastObject = Toast.makeText(
							 * AccelerometerInfoActivity.this,
							 * "Delay rate changed to '" +
							 * delayRatesDescription[position] + "' mode",
							 * Toast.LENGTH_SHORT);
							 * toastObject.setGravity(Gravity.CENTER_VERTICAL |
							 * Gravity.BOTTOM, 0, 0); toastObject.show();
							 */
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {
						// DO NOTHING
					}
				});

	}

	/**
	 * Manages all the check boxes of this Activity.
	 * 
	 * @param view
	 */
	public void onCheckboxClicked(View view) {
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.checkBoxSaveToFile:
			// open the file if set true, otherwise close it.
			if (checked) {
				csvFile = new CsvFileWriter();
				accelerationEventListener.enableSaveToFile();
				accelerationEventListener.setCsvFile(csvFile);
				checkBoxSaveToFile
						.setText(R.string.checkBoxSaveToFileSavingMsg);
				Toast.makeText(
						this,
						getString(R.string.checkBoxSaveToFileSavingMsg) + " "
								+ csvFile.getCaptureFileName(),
						Toast.LENGTH_SHORT).show();
				// TODO test saving the bearing.
				csvLocationFile = new CsvFileWriter();
				myLocationListener.enableSaveToFile();
				myLocationListener.setCsvFile(csvLocationFile);

			} else {
				if (csvFile != null) {
					// Closing the captured file is as important as creating it.
					accelerationEventListener.disableSaveToFile();
					csvFile.closeCaptureFile();
					checkBoxSaveToFile
							.setText(R.string.checkBoxSaveToFileInitialMsg);
					Toast.makeText(
							this,
							getString(R.string.checkBoxSaveToFileStoppedMsg)
									+ " " + csvFile.getCaptureFileName(),
							Toast.LENGTH_SHORT).show();
				}
				if (csvLocationFile != null) {
					// Closing the captured file is as important as creating it.
					myLocationListener.disableSaveToFile();
					csvLocationFile.closeCaptureFile();
				}
			}
			break;
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intentSettings = new Intent(this, Settings_m.class);
			startActivity(intentSettings);
			return true;

		case R.id.search_option:
			if (currentState == Constants.STATE_DISCONNECTED) {
				initializeBluetooth();
				return true;
			} else {
				if (connectedThread != null) {
					connectedThread.cancel();
				}
				return true;
			}

		case R.id.about_option:
			Toast.makeText(this, "Car Brake Detector Demo\nBy Vahid",
					Toast.LENGTH_SHORT).show();
			if (!Constants.BT_MODULE_EXISTS) {
				// initViewsConnectedLinearAcceleration();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * This handler is used to enable communication with the threads.
	 */
	private final Handler mHandler = new Handler() {
		private float bearingValue;

		@Override
		public void handleMessage(Message msg) {
			currentState = msg.what;

			switch (msg.what) {
			case Constants.STATE_CONNECTED:
				// TODO change later
				initViewsConnectedLinearAcceleration();
				// initViewsConnected();
				setStatus(getString(R.string.title_connected) + deviceName);
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
				initViewsNotConnected();
				miSearchOption.setIcon(R.drawable.menu_connect_icon);
				miSearchOption.setTitle(R.string.connect);
				break;
			case Constants.ACCEL_VALUE_MSG:
				linearAccelerationValues = (float[]) msg.obj;
				linearAccelerationMagnitude = AlexMath
						.getVectorMagnitude(linearAccelerationValues);
				// set the value as the text of every TextView
				tvXAxisValue.setText(Float
						.toString(linearAccelerationValues[0]));
				tvYAxisValue.setText(Float
						.toString(linearAccelerationValues[1]));
				tvZAxisValue.setText(Float
						.toString(linearAccelerationValues[2]));
				tvFinalValue.setText(AlexMath.round(
						linearAccelerationMagnitude, 10));

				// set the value on to the SeekBar
				xAxisSeekBar
						.setProgress((int) (linearAccelerationValues[0] + 10f));
				yAxisSeekBar
						.setProgress((int) (linearAccelerationValues[1] + 10f));
				zAxisSeekBar
						.setProgress((int) (linearAccelerationValues[2] + 10f));
				finalSeekBar
						.setProgress((int) (trueAccelerationMagnitude + 10f));
				break;
			case Constants.LOC_VALUE_MSG:
				bearingValue = (Float) msg.obj;
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
		btnConnect.setVisibility(View.GONE);
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
								REQUEST_ENABLE_BT);
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
		byte[] x = AlexMath.toByteArray(acceleromterValues[0]);
		byte[] y = AlexMath.toByteArray(acceleromterValues[1]);
		byte[] z = AlexMath.toByteArray(acceleromterValues[2]);
		byte[] mod_byte = AlexMath.toByteArray(magnitude);
		byte[] xyz_and_Mod = new byte[8 * 4];

		xyz_and_Mod = AlexMath.concatenateBytes(
				AlexMath.concatenateBytes(AlexMath.concatenateBytes(x, y), z),
				mod_byte);
		// ---

		byte[] moduleRealByte = AlexMath.toByteArray(moduleReal);
		byte[] all = new byte[8 * 4 + 8];
		all = AlexMath.concatenateBytes(xyz_and_Mod, moduleRealByte);

		connectedThread.write(all);
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

}
