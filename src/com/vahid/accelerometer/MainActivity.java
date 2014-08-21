package com.vahid.accelerometer;

import java.util.Arrays;
import java.util.Calendar;

import com.vahid.accelerometer.bluetooth.BluetoothDevicesActivity;
import com.vahid.accelerometer.bluetooth.ConnectThread;
import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.util.AlexMath;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;

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
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements SensorEventListener {

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
	// Connected view
	private SeekBar xAxisSeekBar, yAxisSeekBar, zAxisSeekBar, finalSeekBar;
	private TextView tvXAxisValue, tvYAxisValue, tvZAxisValue, tvFinalValue;
	/* the Spinner component for delay rate */
	private Spinner delayRateChooser;
	private CheckBox checkBoxSaveToFile;

	// save to file view fields
	private boolean saveToFileChecked = false;
	private CsvFileWriter csvFile;
	
	
	/**** Sensor related Fields ****/
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mOrientation, mLinearAcceleration, mGravity,
			mMagneticField;
	// Sensor Accelerometer Rates
	private static final int delayRates[] = {
			SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI,
			SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST };
	private static final String delayRatesDescription[] = { "Normal", "UI",
			"Game", "Fastest" };
	private int curDelayRate = SensorManager.SENSOR_DELAY_NORMAL;

	
	// it's important to initialize the values.
	private float[] acceleromterValues = new float[] { 0, 0, 0 };
	private float[] orientationValues = new float[] { 0, 0, 0 };
	private float[] linearAccelerationValues = new float[] { 0, 0, 0 };
	private float[] magneticValues;
	private float[] gravityValues;

	private double linearAccelerationMagnitude;

	
	/**** Rotation Matrix Calculation ****/
	private float[] trueAcceleration = new float[4];
	private float[] inclinationMatrix = new float[16];
	private float[] rotationMatrix = new float[16];
	private float[] rotationMatrixInverse = new float[16];

	private double trueAccelerationMagnitude;

	// --- Filters ---
	boolean brOn = false; // on when is more than one minimum defined
							// (Constant.precision)
	boolean brReal = false; // when the braking is more long than
							// (Constant.marginMilliseconds)
	Calendar brTimeIni = null;

	// ****calculate angles average
	boolean noise = false;
	boolean onAngles = false;
	float sum_angles1 = 0f;
	float sum_angles1_aux = 0f;
	float sum_angles2 = 0f;
	float sum_angles2_aux = 0f;
	int n = 0;
	int n_aux = 0;
	float[] orientationValuesAnterior = new float[] { 0, 0, 0 };

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
				initializeBluetooth();
			}
		});

		// Toast.makeText(getApplicationContext(), "Not connected",
		// Toast.LENGTH_SHORT).show();
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

		unregisterSensors();
	}

	/**
	 * 2nd Important function of this activity. Initializes the views of this
	 * activity when a device gets connected.
	 */
	private void initViewsConnected() {

		Toast.makeText(getApplicationContext(),
				getString(R.string.title_connected) + deviceName,
				Toast.LENGTH_SHORT).show();
		setContentView(R.layout.activity_main);

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].
		if (Constants.BT_MODULE_EXISTS) {
			BluetoothSocket mSocket = connectThread.getBluetoothSocket();
			connectedThread = new ConnectedThread(mSocket, mHandler);
		}

		// we are ready to use the sensor and send the information of the
		// brakes, so...
		initializeSensors();
		registerSensors();

		// tvLASCapturedState = (TextView)
		// findViewById(R.id.textViewLASCapturedstate);

		buttonCheck = (Button) findViewById(R.id.buttonCheck);
		buttonCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				connectedThread.write(AlexMath.toByteArray(60));
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

		// we are ready to use the sensor and send the information of the
		// brakes, so...
		initializeSensors();
		registerSensors();

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

		// populate the spinner
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
						if (curDelayRate != position) {
							curDelayRate = position;
							registerSensors();
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
	 * Method that initializes the sensors, after we're connected to the server.
	 */
	private void initializeSensors() {

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// it is deprecated, but it works.
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		mLinearAcceleration = mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mMagneticField = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "Sensors initialized");

	}

	/**
	 * Register this class as sensor listener with the current delay rate.
	 */
	public void registerSensors() {
		mSensorManager.unregisterListener(this);

		// TODO change this
		/*
		 * mSensorManager.registerListener(this, mAccelerometer,
		 * delayRates[curDelayRate]); mSensorManager.registerListener(this,
		 * mOrientation, delayRates[curDelayRate]);
		 */
		mSensorManager.registerListener(this, mMagneticField,
				delayRates[curDelayRate]);
		mSensorManager.registerListener(this, mGravity,
				delayRates[curDelayRate]);
		mSensorManager.registerListener(this, mLinearAcceleration,
				delayRates[curDelayRate]);
		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "sensors registered");
	}

	/**
	 * Unregisters the sensors on this activity.
	 */
	private void unregisterSensors() {
		try {
			// Be sure that this work in wake_lock (permission to work with
			// screen off)
			if (mSensorManager != null)
				mSensorManager.unregisterListener(this);
			if (Constants.DEBUG)
				Log.d(Constants.LOG_TAG, "sensor unregistered");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
	 * Method that stops everything.
	 */
	private void finilizeAll() {
		initViewsNotConnected(); // #check, ... I put this here because when the
		// orientation changes, the Activity is destroyed, so
		// the device is disconnecting automatically
		unregisterSensors();
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

	}

	public void onCheckboxClicked(View view) {
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.checkBoxSaveToFile:
			saveToFileChecked = checked;
			// open the file if set true, otherwise close it.
			if (checked) {
				csvFile = new CsvFileWriter();
				checkBoxSaveToFile
						.setText(R.string.checkBoxSaveToFileSavingMsg);
			} else {
				if (csvFile != null) {
					// Closing the captured file is as important as creating it.
					csvFile.closeCaptureFile();
					checkBoxSaveToFile
							.setText(R.string.checkBoxSaveToFileInitialMsg);
				}
			}
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// synchronized (this) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			// getAccelerometer(event);
			break;
		case Sensor.TYPE_ORIENTATION:
			// getOrientation(event);
			break;
		case Sensor.TYPE_LINEAR_ACCELERATION:
			getLinearAcceleration2(event);
			// getLinearAcceleration(event);
			break;
		case Sensor.TYPE_GRAVITY:
			getLinearAcceleration2(event);
			// getGravity(event);

			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			getLinearAcceleration2(event);
			// getMagneticField(event);

			break;
		default:
			break;
		}

		// } //fin del syncronized this. //#check?
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
		@Override
		public void handleMessage(Message msg) {
			currentState = msg.what;
			if (msg.what == Constants.STATE_CONNECTED) {
				// TODO change later
				initViewsConnectedLinearAcceleration();
				// initViewsConnected();
				setStatus(getString(R.string.title_connected) + deviceName);
				// change the connect icon on the activity.
				if (miSearchOption != null) {
					miSearchOption.setIcon(R.drawable.menu_disconnect_icon);
					miSearchOption.setTitle(R.string.disconnect);
				}
			} else if (msg.what == Constants.STATE_CONNECTING) {
				TextView tvState = (TextView) findViewById(R.id.textViewNotConnected);
				tvState.setText(R.string.title_connecting);
				setStatus(R.string.title_connecting);
			} else if (msg.what == Constants.STATE_DISCONNECTED) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.msgUnableToConnect),
						Toast.LENGTH_SHORT).show();
				initViewsNotConnected();
				miSearchOption.setIcon(R.drawable.menu_connect_icon);
				miSearchOption.setTitle(R.string.connect);
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
	 * 
	 * 
	 * @param linearAccelerationEvent
	 */
	private void getLinearAcceleration(SensorEvent linearAccelerationEvent) {
		// A three dimensional vector indicating acceleration along each device
		// axis, not including gravity. All values have units of m/s^2. The
		// coordinate system is the same as is used by the acceleration sensor.
		// The
		// output of the accelerometer, gravity and linear-acceleration sensors
		// must
		// obey the following relation:
		//
		//
		// acceleration = gravity + linear-acceleration
		linearAccelerationValues = linearAccelerationEvent.values;

		/*
		 * String linearAccelerations = "Linear Acceleration on the x-axis: " +
		 * linearAccelerationEvent.values[0] +
		 * "\nLinear Acceleration on the y-axis  " +
		 * linearAccelerationEvent.values[1] +
		 * "\nLinear Acceleration on the z-axis " +
		 * linearAccelerationEvent.values[2];
		 * 
		 * tvLASCapturedState.setText(linearAccelerations);
		 */
		// its is enough because you all the acceleration on phone is due to
		// car.If possible apply low pass filter to remove changes due to road
		// bumps .
		linearAccelerationMagnitude = AlexMath
				.getVectorMagnitude(linearAccelerationValues);

		// set the value as the text of every TextView
		tvXAxisValue.setText(Float.toString(linearAccelerationValues[0]));
		tvYAxisValue.setText(Float.toString(linearAccelerationValues[1]));
		tvZAxisValue.setText(Float.toString(linearAccelerationValues[2]));
		tvFinalValue.setText(Double.toString(linearAccelerationMagnitude));

		// If check box for saving the file has been checked.
		if (saveToFileChecked) {
			csvFile.writeToFile(linearAccelerationValues);
			csvFile.writeToFile((float) linearAccelerationMagnitude, true);
		}

		// set the value on to the SeekBar
		xAxisSeekBar.setProgress((int) (linearAccelerationValues[0] + 10f));
		yAxisSeekBar.setProgress((int) (linearAccelerationValues[1] + 10f));
		zAxisSeekBar.setProgress((int) (linearAccelerationValues[2] + 10f));

		finalSeekBar.setProgress((int) (linearAccelerationValues[2]));

	}

	private void getLinearAcceleration2(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
			linearAccelerationValues = event.values;

		if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
			gravityValues = event.values;

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			magneticValues = event.values;

		if (gravityValues != null && magneticValues != null) {
			boolean allGood = SensorManager.getRotationMatrix(rotationMatrix,
					inclinationMatrix, gravityValues, magneticValues);

			if (allGood && linearAccelerationValues != null) {
				/*
				 * float orientation[] = new float[3];
				 * SensorManager.getOrientation(R, orientation);
				 */

				float[] linearAccelerationValuesNew = {
						linearAccelerationValues[0],
						linearAccelerationValues[1],
						linearAccelerationValues[2], 0 };

				Matrix.invertM(rotationMatrixInverse, 0, rotationMatrix, 0);
				Matrix.multiplyMV(trueAcceleration, 0, rotationMatrixInverse,
						0, linearAccelerationValuesNew, 0);

				// This is Magnetic North in Radians
				// bearing = orientation[0];

				// ATTENTION: The Bearing will change based on Screen
				// Orientation!!!
				// Tilt your device and see what I mean ...see?
				// Outside the scope of this sample, but easy to compensated
				// for.

				// Convert to degrees & 360 span (feel like I'm in Trig class
				// with Radians)
				// bearing = ((float) Math.toDegrees(bearing) + 360) % 360;

				// if you need True North enable the lines below and you'll need
				// GPS
				// Lat, Lng, Alt for it to work though. It will give you a value
				// in degrees
				// you'll need to subtract from Magnetic North. And, once again
				// Modulus to 360.
				// GeomagneticField geoField = new GeomagneticField(
				// (float) currentLatLng.latitude,
				// (float) currentLatLng.longitude,
				// (float) currentAlt,
				// System.currentTimeMillis());
				// trueBearing = bearing - geoField.getDeclination();
				// trueBearing = (trueBearing + 360) % 360

				// Update GUI (in degrees 0.f-360.f)
				/*
				 * currentBearing.setText(Float.toString(bearing)+ "\n" +
				 * Float.toString(orientation[1]) + "\n" +
				 * Float.toString(orientation[2]) + "\n");
				 */
				linearAccelerationMagnitude = AlexMath
						.getVectorMagnitude(linearAccelerationValues);
				trueAccelerationMagnitude = AlexMath
						.getVectorMagnitude(trueAcceleration);

				tvXAxisValue.setText(Float.toString(trueAcceleration[0]));
				tvYAxisValue.setText(Float.toString(trueAcceleration[1]));
				tvZAxisValue.setText(Float.toString(trueAcceleration[2]));
				tvFinalValue.setText(Double
						.toString((float)trueAccelerationMagnitude));

				// set the value on to the SeekBar
				xAxisSeekBar.setProgress((int) (trueAcceleration[0] + 10f));
				yAxisSeekBar.setProgress((int) (trueAcceleration[1] + 10f));
				zAxisSeekBar.setProgress((int) (trueAcceleration[2] + 10f));

				finalSeekBar
						.setProgress((int) (trueAccelerationMagnitude));

				// If check box for saving the file has been checked.
				if (saveToFileChecked) {
					// write the values of the linear acceleration
					csvFile.writeToFile(linearAccelerationValues);
					csvFile.writeToFile((float) linearAccelerationMagnitude,
							false);
					// write the values of the true acceleration
					csvFile.writeToFile(Arrays.copyOfRange(trueAcceleration, 0,
							trueAcceleration.length - 1));
					csvFile.writeToFile((float) trueAccelerationMagnitude, true);
				}

			}
		}
	}

	private void getMagneticField(SensorEvent event) {
		magneticValues = event.values;
	}

	private void getGravity(SensorEvent event) {
		gravityValues = event.values;
	}

	private void getAccelerometer(SensorEvent event) {
		acceleromterValues = AlexMath.cancelGravity(event.values,
				orientationValues); // the sensor doesn't erase the
									// gravity by itself
									// for that exists other sensor:
									// LINEAR_ACCELERATION, but is not
									// very typical to have it.

		/*
		 * we don't need to cancel the gravity, because we are going to use the
		 * axes x0,y0,z0, in which angles the gravity are all it in z0
		 */// but we do... the results appears to be better.

		// **acceleromterValues = event.values.clone();
		acceleromterValues = AlexMath.convertReference(acceleromterValues,
				orientationValues); // **

		// float[] temp = { acceleromterValues[0], acceleromterValues[1], 0 };
		// double magnitude = AlexMath.getVectorMagnitude(temp);
		double magnitude = AlexMath.getVectorMagnitude(acceleromterValues);

		// *******first filter of braking.

		// *********braking????*********
		boolean braking = false;
		if (acceleromterValues[1] > 0
				&& Math.abs(orientationValues[1]) < 90 + Constants.precisionPitch) {
			braking = true;
		} else if (acceleromterValues[1] < 0
				&& Math.abs(orientationValues[1]) > 90 + Constants.precisionPitch) {
			braking = true;
		}

		boolean accelerating = false;
		if (acceleromterValues[1] < 0 && Math.abs(orientationValues[1]) < 90) {
			accelerating = true;
		} else if (acceleromterValues[1] > 0
				&& Math.abs(orientationValues[1]) > 90) {
			accelerating = true;
		}
		// ******end***braking?????****

		// if (module>Constant.precision && acceleromterValues[1]>0){
		if (magnitude > Constants.precision && braking) { // braking is the
															// boolean
															// variable that
															// defines the
															// brake and the
															// acceleration
															// with the sign
															// of y
			RelativeLayout background = (RelativeLayout) findViewById(R.id.connectedLayout);
			background.setBackgroundResource(R.color.dark_red);

			if (brOn == false) {
				brOn = true;
				brReal = false;
				brTimeIni = Calendar.getInstance();

			} else {
				if (Calendar.getInstance().getTimeInMillis()
						- brTimeIni.getTimeInMillis() > Constants.marginMilliseconds) {
					brReal = true;
					try {
						// connected.write((int) (10*module));
						// connected.write(mmath.toByteArray(module));

					} catch (Exception e) {
						Toast.makeText(getApplicationContext(),
								"error writting", Toast.LENGTH_SHORT).show();
					}
					TextView tvaux = (TextView) findViewById(R.id.textViewMain);
					tvaux.setText("" + AlexMath.round(magnitude, 1));
					ProgressBar progress = (ProgressBar) findViewById(R.id.seekBar1);
					progress.setProgress((int) magnitude);

				} else {
					brReal = false;
				}

			}

		}// end br starts
		else {
			// *****
			TextView tvaux = (TextView) findViewById(R.id.textViewMain);
			tvaux.setText("");
			// --
			ProgressBar progress = (ProgressBar) findViewById(R.id.seekBar1);
			progress.setProgress((int) (0));
			// *****
			brReal = false;
			brOn = false;
			RelativeLayout backg = (RelativeLayout) findViewById(R.id.connectedLayout);
			// if (acceleromterValues[1]<0 &&
			// module>Constant.precision){
			if (accelerating && magnitude > Constants.precision) {
				backg.setBackgroundResource(R.color.dark_green);
			} else {
				backg.setBackgroundColor(Color.WHITE);

			}

		}// end 'end brake'

		// ******end the first filter to brakings

		/**
		 * ******WRITE TO DE BLUETOOTH DEVICE*********
		 */

		// ****writing also the module when brake is real.
		double moduleReal;
		if (brReal) {
			moduleReal = magnitude;
		} else {
			moduleReal = 0;

		}

		// ---with the idea of write this data and send by bluetooth,
		// first it's necessary to pass them to byte...
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

		/**
		 * ******
		 */
	}

	private void getOrientation(SensorEvent event) {
		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "copy Orientation");

		// see the values in phone screen (debug)
		String angles = "azimuth: " + AlexMath.round(event.values[0], 3)
				+ "\npitch:     " + AlexMath.round(event.values[1], 3)
				+ "\nroll:       " + AlexMath.round(event.values[2], 3);
		TextView tv = (TextView) findViewById(R.id.textViewConnected);
		tv.setText(angles);

		if (onAngles == false) {
			orientationValues = event.values.clone();
			orientationValuesAnterior = event.values.clone();
			onAngles = true;
		}
		orientationValues[0] = event.values[0];

		// ****calculate angles average

		float delta1 = event.values[1] - orientationValuesAnterior[1];
		float delta2 = event.values[2] - orientationValuesAnterior[2];
		float delta = Math.max(delta1, delta2);

		// .1
		if (delta < Constants.delta && noise == false) {
			sum_angles1 = sum_angles1 + event.values[1];
			sum_angles2 = sum_angles2 + event.values[2];
			n++;
			orientationValues[1] = (float) sum_angles1 / n;
			orientationValues[2] = (float) sum_angles2 / n;
			if (n == Constants.nIter) {

				sum_angles1 = event.values[1];
				sum_angles2 = event.values[2];
				n = 1;
			}
		} else if (delta > Constants.delta) {

			noise = true;
			sum_angles1_aux = 0;
			n = 0;
		} else if (delta < Constants.delta && noise == true) {
			sum_angles1_aux = sum_angles1_aux + event.values[1];
			sum_angles2_aux = sum_angles2_aux + event.values[2];
			n++;
			if (n == Constants.nIter) {
				orientationValues[1] = (float) sum_angles1_aux / n;
				orientationValues[2] = (float) sum_angles2_aux / n;

				sum_angles1 = event.values[1];
				sum_angles2 = event.values[2];
				n = 1;
				noise = false;
			}
		}

		// .2
		// ****end***calculate angles average
		orientationValuesAnterior = event.values.clone();
	}// end case sensor Orientation

}
