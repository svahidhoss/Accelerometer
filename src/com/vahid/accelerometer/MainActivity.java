package com.vahid.accelerometer;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vahid.accelerometer.bluetooth.BluetoothDevicesActivity;
import com.vahid.accelerometer.bluetooth.ConnectThread;
import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.util.MathUtil;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.accelerometer.util.MovingAverage;
import com.vahid.accelerometer.util.MovingAverageTime;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements Runnable {

	/**** for communication between activities ****/
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_CONNECT_DEVICE = 2;

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
	private LinearLayout mBackground;
	private Button btnConnect, btnCheck;
	private TextView tvState;
	private MenuItem miSearchOption;

	// 2. Connected views
	// private SeekBar xAxisSeekBar, yAxisSeekBar, zAxisSeekBar;
	private ProgressBar mFinalProgressBar;
	private TextView tvXAxisValue, tvYAxisValue, tvZAxisValue, tvFinalValue;
	private TextView tvXTrueAxisValue, tvYTrueAxisValue, tvZTrueAxisValue,
			tvRotationDegreeValue, tvAccelerationDegreeValue, tvBrake,
			tvBrakeValue;
	/* the Spinner component for delay rate */
	private Spinner delayRateChooser;
	private CheckBox checkBoxSaveToFile;

	/**** save to file view fields ****/
	private CsvFileWriter mCsvSensorsFile, mCsvLocationFile;

	/**** Location Related fields ****/
	private MyLocationListener myLocationListener;
	private LocationManager mLocationManager;
	private ScheduledExecutorService mGpsExecutor;

	/**** Sensor related Fields ****/
	// private SensorManager mSensorManager;
	private AccelerationEventListener mAccelerationEventListener;
	private int mCurrentDelayRate = SensorManager.SENSOR_DELAY_NORMAL;

	// Sensor Values: it's important to initialize them.
	private float[] acceleromterValues = new float[] { 0, 0, 0 };
	// private float[] orientationValues = new float[] { 0, 0, 0 };
	private float[] earthLinearAccelerationValues = new float[] { 0, 0, 0 };
	// private float[] trueLinearAccelerationValues = new float[] { 0, 0, 0 };
	private double mLinearAccelerationMagnitude;

	// Calculation of Motion Direction for brake detection
	private float mCurrentMovementBearing, mCurrentAccelerationBearing;

	// current situation of the activity.
	private int mAccelSituation = Constants.NO_MOVE_DETECTED;

	// Moving Averages
	private MovingAverage elaMovingAverageX, elaMovingAverageY,
			elaMovingAverageZ;
	private MovingAverage laMagMovingAverage;
	private MovingAverage tlaMovingAverageX, tlaMovingAverageY,
			tlaMovingAverageZ;
	private MovingAverage mCurAccBearingMovingAverage,
			mCurMovBearingMovingAverage;

	// used for putting values in this list to execute the average deceleration
	// after it exceeds the certain threshold. It's cleaned when we face an
	// acceleration again.
	private MovingAverageTime decelerationMovingAverageTime;

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

		if (mAccelerationEventListener != null) {
			mAccelerationEventListener.unregisterSensors();
		}
	}

	/**
	 * 2nd Important function of this activity. Initializes the views of this
	 * activity when a device gets connected.
	 */
	private void initViewsConnected() {

		Toast.makeText(getApplicationContext(),
				getString(R.string.title_connected) + mDeviceName,
				Toast.LENGTH_SHORT).show();
		setContentView(R.layout.activity_main_connected);

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].
		if (Constants.BT_MODULE_EXISTS) {
			BluetoothSocket mSocket = mConnectThread.getBluetoothSocket();
			mConnectedThread = new ConnectedThread(mSocket, mHandler);
		}

		// we are ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerationEventListener = new AccelerationEventListener(mHandler);
		mAccelerationEventListener.initializeSensors(mSensorManager);
		mAccelerationEventListener.registerSensors(mCurrentDelayRate);

		// TODO what is this really doing?
		btnCheck = (Button) findViewById(R.id.buttonCheck);
		btnCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Constants.BT_MODULE_EXISTS)
					mConnectedThread.write(MathUtil.toByteArray(60));
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
				getString(R.string.title_connected) + mDeviceName,
				Toast.LENGTH_SHORT).show();

		setContentView(R.layout.activity_main_las);
		// initiate moving averages
		initiateMovingAverages();

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].
		if (Constants.BT_MODULE_EXISTS) {
			BluetoothSocket mSocket = mConnectThread.getBluetoothSocket();
			mConnectedThread = new ConnectedThread(mSocket, mHandler);

		}

		// we are ready for GPS
		if (Constants.GPS_MODULE_EXISTS) {
			myLocationListener = new MyLocationListener(
					getApplicationContext(), mHandler);
			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					Constants.GPS_MIN_TIME_MIL_SEC,
					Constants.GPS_MIN_DISTANCE_METER, myLocationListener);
			// Creates a thread pool of size 1 to schedule commands to run
			// periodically
			mGpsExecutor = Executors.newScheduledThreadPool(1);
			mGpsExecutor.scheduleAtFixedRate(this, 0, Constants.RUNNING_PERIOD,
					TimeUnit.MILLISECONDS);
			// TODO check this?
			// provider = myLocationManager.getBestProvider(criteria, false);
			// Location loc = myLocationManager
			// .getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}

		// we are also ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerationEventListener = new AccelerationEventListener(mHandler);
		mAccelerationEventListener.initializeSensors(mSensorManager);
		mAccelerationEventListener.registerSensors(mCurrentDelayRate);

		// retrieve all the needed components
		// TODO correct later
		/*
		 * xAxisSeekBar = (SeekBar) findViewById(R.id.xAxisBar); yAxisSeekBar =
		 * (SeekBar) findViewById(R.id.yAxisBar); zAxisSeekBar = (SeekBar)
		 * findViewById(R.id.zAxisBar);
		 */
		mBackground = (LinearLayout) findViewById(R.id.activity_main_las);

		tvXAxisValue = (TextView) findViewById(R.id.xAxisValue);
		tvYAxisValue = (TextView) findViewById(R.id.yAxisValue);
		tvZAxisValue = (TextView) findViewById(R.id.zAxisValue);

		tvFinalValue = (TextView) findViewById(R.id.finalValue);
		mFinalProgressBar = (ProgressBar) findViewById(R.id.finalProgressBar);

		tvXTrueAxisValue = (TextView) findViewById(R.id.xAxisTrueValue);
		tvYTrueAxisValue = (TextView) findViewById(R.id.yAxisTrueValue);
		tvZTrueAxisValue = (TextView) findViewById(R.id.zAxisTrueValue);

		tvRotationDegreeValue = (TextView) findViewById(R.id.rotationDegreeeValue);
		tvAccelerationDegreeValue = (TextView) findViewById(R.id.accelerationDegreeeValue);

		tvBrake = (TextView) findViewById(R.id.brakeTextView);
		tvBrakeValue = (TextView) findViewById(R.id.brakeValueTextView);

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
						.equals(action)) {
					// check this
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

		if (Constants.GPS_MODULE_EXISTS) {
			// shutdown the GPS Executor immediately
			if (mGpsExecutor != null) {
				mGpsExecutor.shutdown();
			}

			// Remove the listener you previously added to location manager, if
			// not
			// null, e.g. GPS not used in debugging
			if (mLocationManager != null) {
				mLocationManager.removeUpdates(myLocationListener);
			}
		}

		if (mAccelerationEventListener != null) {
			mAccelerationEventListener.unregisterSensors();
		}
		// disconnect the thread first
		if (mConnectedThread != null) {
			mConnectThread.cancel();
		}

		// unregister the receivers
		if (mReceiver != null) {
			this.unregisterReceiver(mReceiver);
		}

		// close the captured file if not already
		if (mCsvSensorsFile != null) {
			mCsvSensorsFile.closeCaptureFile();
			checkBoxSaveToFile.setText(R.string.checkBoxSaveToFileInitialMsg);
		}

		if (mCsvLocationFile != null) {
			mCsvLocationFile.closeCaptureFile();
		}

		initViewsNotConnected();
		// #check, ... I put this here because when the
		// orientation changes, the Activity is destroyed, so
		// the device is disconnecting automatically
	}

	/**
	 * Method to add values to the delay rate spinner.
	 */
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
						if (mCurrentDelayRate != position) {
							mCurrentDelayRate = position;
							if (mAccelerationEventListener != null) {
								mAccelerationEventListener
										.registerSensors(mCurrentDelayRate);
							}
							// show a toast message
							Toast.makeText(
									getApplicationContext(),
									"Delay rate changed to '"
											+ Constants.DELAY_RATES_DESCRIPTION[position]
											+ "' mode", Toast.LENGTH_SHORT)
									.show();
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
				mCsvSensorsFile = new CsvFileWriter("Sensors");
				mAccelerationEventListener.enableSaveToFile();
				mAccelerationEventListener.setCsvFile(mCsvSensorsFile);
				checkBoxSaveToFile
						.setText(R.string.checkBoxSaveToFileSavingMsg);
				Toast.makeText(
						this,
						getString(R.string.checkBoxSaveToFileSavingMsg) + " "
								+ mCsvSensorsFile.getCaptureFileName(),
						Toast.LENGTH_SHORT).show();
				if (myLocationListener != null) {
					// created the file for saving location information
					mCsvLocationFile = new CsvFileWriter("Location");
					myLocationListener.enableSaveToFile();
					myLocationListener.setCsvFile(mCsvLocationFile);
				}

			} else {
				// Closing the logging files as it had the same importance as
				// creating them.
				if (mCsvSensorsFile != null) {
					mAccelerationEventListener.disableSaveToFile();
					mCsvSensorsFile.closeCaptureFile();
					checkBoxSaveToFile
							.setText(R.string.checkBoxSaveToFileInitialMsg);
					Toast.makeText(
							this,
							getString(R.string.checkBoxSaveToFileStoppedMsg)
									+ " "
									+ mCsvSensorsFile.getCaptureFileName(),
							Toast.LENGTH_SHORT).show();
				}
				if (mCsvLocationFile != null) {
					myLocationListener.disableSaveToFile();
					mCsvLocationFile.closeCaptureFile();
				}
			}
			break;

		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intentSettings = new Intent(this, SettingsActivity.class);
			startActivity(intentSettings);
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
			mCurrentBTState = msg.what;

			switch (msg.what) {
			case Constants.STATE_CONNECTED:
				// TODO change later
				initViewsConnectedLinearAcceleration();
				// initViewsConnected();
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
				initViewsNotConnected();
				miSearchOption.setIcon(R.drawable.menu_connect_icon);
				miSearchOption.setTitle(R.string.connect);
				break;
			case Constants.ACCEL_VALUE_MSG:
				// 1. Receive the linear acceleration values
				earthLinearAccelerationValues = (float[]) msg.obj;

				// 2. calculate the actual acceleration bearing
				elaMovingAverageX.pushValue(earthLinearAccelerationValues[0]);
				elaMovingAverageY.pushValue(earthLinearAccelerationValues[1]);
				elaMovingAverageZ.pushValue(earthLinearAccelerationValues[2]);
				
				mCurrentAccelerationBearing = MathUtil
						.calculateCurrentAccelerationBearing(elaMovingAverageY.getMovingAverage(), elaMovingAverageX.getMovingAverage());

				// 3.Update the UI (set the value ) as the text of every
				// TextView
				tvXAxisValue.setText(Float.toString(elaMovingAverageX
						.getMovingAverage()));
				tvYAxisValue.setText(Float.toString(elaMovingAverageY
						.getMovingAverage()));
				tvZAxisValue.setText(Float.toString(elaMovingAverageZ
						.getMovingAverage()));

				// 4. calculate the linear acceleration mag. (-z)
				mLinearAccelerationMagnitude = MathUtil
						.getVectorMagnitudeMinusZ(earthLinearAccelerationValues);
				// laMagMovingAverage
				// .pushValue((float) mLinearAccelerationMagnitude);

				// 5. updating the UI with Acceleration Magnitude
				tvFinalValue.setText(Float
						.toString((float) mLinearAccelerationMagnitude));
				int progressPercentage = (int) (mLinearAccelerationMagnitude * 5);
				mFinalProgressBar.setProgress(progressPercentage);

				// 6. Detect the situation
				displayDetectedSituation(
						// mCurAccBearingMovingAverage.getMovingAverage(),
						// mCurMovBearingMovingAverage.getMovingAverage(),
						mCurrentMovementBearing, mCurrentAccelerationBearing,
						mLinearAccelerationMagnitude);

				// TODO we don't need this rotation imho
				/*
				 * trueLinearAccelerationValues = AlexMath.convertReference(
				 * earthLinearAccelerationValues, mCurrentMovementBearing);
				 * tlaMovingAverageX.pushValue(trueLinearAccelerationValues[0]);
				 * tlaMovingAverageY.pushValue(trueLinearAccelerationValues[1]);
				 * tlaMovingAverageZ.pushValue(trueLinearAccelerationValues[2]);
				 * 
				 * // set the value as the text of every TextView
				 * tvXTrueAxisValue.setText(Float.toString(tlaMovingAverageX
				 * .getMovingAverage()));
				 * tvYTrueAxisValue.setText(Float.toString(tlaMovingAverageY
				 * .getMovingAverage())); // display the current situation if
				 * there's a brake.
				 * displayDetectedSituation(tlaMovingAverageX.detectSituation(),
				 * tlaMovingAverageY.detectSituation());
				 * 
				 * tvZTrueAxisValue.setText(Float.toString(tlaMovingAverageZ
				 * .getMovingAverage()));
				 */
				tvRotationDegreeValue.setText(Float
						.toString(mCurrentMovementBearing));
				tvAccelerationDegreeValue.setText(Float
						.toString(mCurrentAccelerationBearing));

				break;
			case Constants.MOVEMENT_BEARING_MSG:
				// TODO later change it to detect no movement.
				// only use the bearing if it's not zero
				if (msg.arg1 != 0) {
					mCurrentMovementBearing = Math.abs((Float) msg.obj);
					// mCurMovBearingMovingAverage
					// .pushValue(mCurrentMovementBearing);
				}
				break;
			case Constants.BRAKE_DETECTED_MSG:
				// float f = (Float) msg.obj;
				tvBrake.setText("Brake Detected!");
				// tvBrakeValue.setText(Float.toString(f));
				mAccelSituation = Constants.BRAKE_DETECTED;
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
		byte[] x = MathUtil.toByteArray(acceleromterValues[0]);
		byte[] y = MathUtil.toByteArray(acceleromterValues[1]);
		byte[] z = MathUtil.toByteArray(acceleromterValues[2]);
		byte[] mod_byte = MathUtil.toByteArray(magnitude);
		byte[] xyz_and_Mod = new byte[8 * 4];

		xyz_and_Mod = MathUtil.concatenateBytes(
				MathUtil.concatenateBytes(MathUtil.concatenateBytes(x, y), z),
				mod_byte);
		// ---

		byte[] moduleRealByte = MathUtil.toByteArray(moduleReal);
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
	 * Show weather a brake or acceleration has occurred, it considers if the
	 * acceleration magnitude on y (North) and x (East) is more than a certain
	 * threshold and if it's been going on for more than a certain time.
	 * 
	 * @param accelerationBearing
	 * @param movementBearing
	 * @param linearAccelMagMinusZ
	 */
	private void displayDetectedSituation(float accelerationBearing,
			float movementBearing, double linearAccelMagMinusZ) {
		float bearingDifference = MathUtil.getBearingsAbsoluteDifference(
				accelerationBearing, movementBearing);

		if (linearAccelMagMinusZ >= Constants.ACCEL_THRESHOLD) {
			if (bearingDifference > Constants.DIFF_DEGREE) {
				mAccelSituation = Constants.BRAKE_DETECTED;
				decelerationMovingAverageTime.pushValue(
						(float) linearAccelMagMinusZ, new Date());
			} else {
				mAccelSituation = Constants.ACCEL_DETECTED;
				decelerationMovingAverageTime.clearValues();
			}
		} else {
			mAccelSituation = Constants.NO_MOVE_DETECTED;
			decelerationMovingAverageTime.clearValues();
		}

		switch (mAccelSituation) {
		case Constants.BRAKE_DETECTED:
			mBackground.setBackgroundResource(R.color.dark_red);
			/*
			 * mFinalProgressBar.setProgressDrawable(getResources().getDrawable(
			 * R.drawable.progress_bar_vahid_red));
			 */
			break;
		case Constants.ACCEL_DETECTED:
			mBackground.setBackgroundResource(R.color.dark_green);
			/*
			 * mFinalProgressBar.setProgressDrawable(getResources().getDrawable(
			 * R.drawable.progress_bar_vahid_green));
			 */
			break;
		default:
			mBackground.setBackgroundResource(R.color.White);
			break;
		}
	}

	/**
	 * Initiating moving averages function for better management.
	 */
	private void initiateMovingAverages() {
		// earth linear acceleration initiating
		elaMovingAverageX = new MovingAverage(Constants.WINDOW_SIZE);
		elaMovingAverageY = new MovingAverage(Constants.WINDOW_SIZE);
		elaMovingAverageZ = new MovingAverage(Constants.WINDOW_SIZE);

		// true linear acceleration initiating
		tlaMovingAverageX = new MovingAverage(Constants.WINDOW_SIZE);
		tlaMovingAverageY = new MovingAverage(Constants.WINDOW_SIZE);
		tlaMovingAverageZ = new MovingAverage(Constants.WINDOW_SIZE);

		// used for smoothing the linear acceleration mag.
		laMagMovingAverage = new MovingAverage(Constants.WINDOW_SIZE);

		// used for smoothing the bearing values.
		mCurAccBearingMovingAverage = new MovingAverage(Constants.WINDOW_SIZE);
		mCurMovBearingMovingAverage = new MovingAverage(Constants.WINDOW_SIZE);

		decelerationMovingAverageTime = new MovingAverageTime(
				Constants.WINDOW_SIZE_IN_MILI_SEC, mHandler);
	}

	@Override
	public void run() {
		// TODO
		// displayDetectedSituation(mCurrentAccelerationBearing,
		// mCurrentMovementBearing, mLinearAccelerationMagnitude);
	}
}
