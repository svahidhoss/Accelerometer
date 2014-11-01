package com.vahid.accelerometer;

import java.util.Calendar;

import com.vahid.accelerometer.util.MathUtil;
import com.vahid.accelerometer.util.Constants;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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


	/**** Defining view fields ****/
	// 1.Initial views
	private Button btnConnectBT, btnCheck, btnRunBarsActivity;
	private TextView tvState;

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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * 1st Important function of this activity. Initializes the views of this
	 * activity when no device is connected.
	 */
	private void initViews() {
		setContentView(R.layout.activity_main);

		btnConnectBT = (Button) findViewById(R.id.btnConnectBT);
		btnRunBarsActivity = (Button) findViewById(R.id.btnRunBarsAct);

		tvState = (TextView) findViewById(R.id.textViewNotConnected);

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
//			if (Constants.BT_MODULE_EXISTS)
//				initializeBluetooth();
//			else {
				runConnectedDebugActivity();
//				return;
//			}
			break;
		case R.id.btnRunBarsAct:
			// open the file if set true, otherwise close it.
			runConnectedBarsActivity();
			break;
		}
	}

	
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


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_option:
			Intent intentSettings = new Intent(this, SettingsActivity.class);
			startActivityForResult(intentSettings,
					Constants.REQUEST_SETTINGS_CHANGE);
			return true;

		case R.id.about_option:
			Toast.makeText(this, "Car Brake Detector Demo\nBy Vahid",
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

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

//		mConnectedThread.write(all);
		
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

}
