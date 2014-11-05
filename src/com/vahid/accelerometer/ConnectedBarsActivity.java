package com.vahid.accelerometer;

import java.util.Date;

import com.vahid.accelerometer.bluetooth.BluetoothDevicesActivity;
import com.vahid.accelerometer.bluetooth.ConnectThread;
import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.filter.MovingAverage;
import com.vahid.accelerometer.filter.MovingAverage2;
import com.vahid.accelerometer.filter.MovingAverageTime;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.accelerometer.util.MathUtil;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ConnectedBarsActivity extends Activity {
	/**** Defining view fields ****/
	private MenuItem miSearchOption;
	// 2. Connected views
	private ProgressBar mAccelProgressBar, mBrakeProgressBar;
	private TextView tvXAxisValue, tvYAxisValue, tvFinalValue;
	private TextView tvRotationDegreeValue, tvAccelerationDegreeValue, tvBrake,
			tvBrakeValue;
	/* the Spinner component for delay rate */
	private Spinner delayRateChooser;
	private CheckBox checkBoxSaveToFile;

	/**** Bluethooth related fields ****/
	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mBroadcastReceiver;

	private ConnectThread mConnectThread = null;
	private ConnectedThread mConnectedThread = null;
	private String mDeviceName = "";

	private static int mCurrentBTState = Constants.STATE_DISCONNECTED;

	/**** Save to file view fields ****/
	private CsvFileWriter mCsvSensorsFile, mCsvLocationFile, mCsvProcessedFile;
	// boolean to check if it should save to mCsvProcessedFile;
	private boolean mSavingToProcessedFile = false;
	// The Runnable task to detect if there's a brake and save to file.
	// private Runnable mDisplayDetectedSituationTask;

	/**** Location Related fields ****/
	private MyLocationListener myLocationListener;
	private LocationManager mLocationManager;

	/**** Sensor Related Fields ****/
	// private SensorManager mSensorManager;
	private AccelerationEventListener mAccelerationEventListener;
	private int mCurrentDelayRate = SensorManager.SENSOR_DELAY_NORMAL;

	// Sensor Values: it's important to initialize them.

	// private float[] orientationValues = new float[] { 0, 0, 0 };
	private float[] earthLinearAccelerationValues = new float[] { 0, 0, 0 };
	// private float[] trueLinearAccelerationValues = new float[] { 0, 0, 0 };
	private double mLinearAccelerationMagnitude;

	// Calculation of Motion Direction for brake detection
	private float mCurrentMovementBearing, mCurrentAccelerationBearing;

	// current situation of the activity.
	private int mAccelSituation = Constants.NO_MOVE_DETECTED;

	// Moving Averages
	private MovingAverage2 elaMovingAverageX, elaMovingAverageY;
	private MovingAverage laMagMovingAverage, mCurAccBearingMovingAverage,
			mCurMovBearingMovingAverage;

	// Used for loading values in this list to execute the average deceleration
	// after it exceeds the certain threshold. It's cleaned after an
	// acceleration again.
	private MovingAverageTime decelerationMovingAverageTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initiateViews();

		if (Constants.BT_MODULE_EXISTS) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			setStatus(R.string.title_not_connected);
			initializeBluetooth();
		} else {
			// if no bluetooth needed go straight to using sensors.
			initiateSensors();
		}

		// we are ready for GPS, Only used when we have the GPS.
		if (Constants.GPS_MODULE_EXISTS) {
			myLocationListener = new MyLocationListener(
					getApplicationContext(), mHandler);
			activateLocationUpdatesFromGPS();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the connected_menu; this adds items to the action bar if it
		// is present.
		getMenuInflater().inflate(R.menu.connected_menu, menu);
		// set the miSearchOption as the first item of the menu.
		miSearchOption = menu.getItem(1);
		return true;
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		// when movement bearing information comes manually from the settings
		// activity. (when GPS is deactivated).
		case Constants.REQUEST_SETTINGS_CHANGE:
			if (resultCode == RESULT_OK) {
				mCurrentMovementBearing = Math.abs(data.getExtras().getFloat(
						SettingsActivity.SET_BEARING));
			}
			break;

		case Constants.REQUEST_CONNECT_DEVICE:
			if (resultCode == RESULT_OK) {
				String deviceAddress = data.getExtras().getString(
						BluetoothDevicesActivity.EXTRA_DEVICE_ADDRESS);
				mDeviceName = data.getExtras().getString(
						BluetoothDevicesActivity.EXTRA_DEVICE_NAME);
				// need to connect to device here
				connectBluetoothDevice(deviceAddress);
			}
			if (resultCode == RESULT_CANCELED) {
				// TODO
				// was not able to connect, do you want to continue anyway?
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// unregister the receivers
		// if (mBroadcastReceiver != null) {
		// unregisterReceiver(mBroadcastReceiver);
		// }
	}

	@Override
	protected void onStop() {
		super.onStop();

		// disconnect the thread first
		if (mConnectedThread != null) {
			mConnectThread.cancel();
		}

		// unregister the receivers
		if (mBroadcastReceiver != null) {
			unregisterReceiver(mBroadcastReceiver);
		}

		if (Constants.GPS_MODULE_EXISTS) {
			deactivateLocationUpdatesFromGPS();
		}

		if (mAccelerationEventListener != null) {
			mAccelerationEventListener.unregisterSensors();
		}

		// close the captured file if not already
		if (mCsvSensorsFile != null) {
			mCsvSensorsFile.closeCaptureFile();
			checkBoxSaveToFile.setText(R.string.checkBoxSaveToFileInitialMsg);
		}

		if (mCsvLocationFile != null) {
			mCsvLocationFile.closeCaptureFile();
		}

		if (mCsvProcessedFile != null) {
			mCsvProcessedFile.closeCaptureFile();
		}
	}

	/**
	 * 2nd Important function of this activity. Initializes the views of this
	 * activity when a device gets connected.
	 */
	private void initiateViews() {
		setContentView(R.layout.activity_connected_bars);
		// initiate moving averages for filtering sensor values.
		initiateMovingAverages();

		tvXAxisValue = (TextView) findViewById(R.id.xAxisValue);
		tvYAxisValue = (TextView) findViewById(R.id.yAxisValue);

		tvFinalValue = (TextView) findViewById(R.id.finalValue);

		mBrakeProgressBar = (ProgressBar) findViewById(R.id.brakeProgressBar);
		mAccelProgressBar = (ProgressBar) findViewById(R.id.accelProgressBar);

		tvRotationDegreeValue = (TextView) findViewById(R.id.rotationDegreeeValue);
		tvAccelerationDegreeValue = (TextView) findViewById(R.id.accelerationDegreeeValue);

		// tvBrake = (TextView) findViewById(R.id.brakeTextView);
		// tvBrakeValue = (TextView) findViewById(R.id.brakeValueTextView);

		checkBoxSaveToFile = (CheckBox) findViewById(R.id.checkBoxSaveToFile);

		// populate the delay rate spinner
		populateDelayRateSpinner();
	}

	/**
	 * Initiating moving averages function for better management.
	 */
	private void initiateMovingAverages() {
		// earth linear acceleration initiating
		elaMovingAverageX = new MovingAverage2(
				Constants.WINDOW_SIZE_MEDIAN_FILTER);
		elaMovingAverageY = new MovingAverage2(
				Constants.WINDOW_SIZE_MEDIAN_FILTER);

		// used for smoothing the linear acceleration mag.
		laMagMovingAverage = new MovingAverage(Constants.WINDOW_SIZE_SMA_FILTER);

		// used for smoothing the bearing values.
		mCurAccBearingMovingAverage = new MovingAverage(
				Constants.WINDOW_SIZE_SMA_FILTER);
		mCurMovBearingMovingAverage = new MovingAverage(
				Constants.WINDOW_SIZE_SMA_FILTER);

		decelerationMovingAverageTime = new MovingAverageTime(
				Constants.WINDOW_SIZE_IN_MILI_SEC, mHandler);
	}

	/**
	 * Initiating the needed sensors for sensing brakes.
	 */
	private void initiateSensors() {
		// we are also ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerationEventListener = new AccelerationEventListener(mHandler);
		mAccelerationEventListener.initializeSensors(mSensorManager);
		mAccelerationEventListener.registerSensors(mCurrentDelayRate);
	}

	/**
	 * Method to add values to the delay rate spinner for layout implementation.
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
		String displayMsg;

		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.checkBoxSaveToFile:
			// open the file if set true, otherwise close it.
			if (checked) {
				displayMsg = getString(R.string.checkBoxSaveToFileSavingMsg);
				// 1.write unprocessed values of linear acceleration values
				mCsvSensorsFile = new CsvFileWriter("Sensors");
				mAccelerationEventListener.enableSaveToFile();
				mAccelerationEventListener.setCsvFile(mCsvSensorsFile);
				displayMsg += "\n" + mCsvSensorsFile.getCaptureFileName();

				// 2.Process file that saves values of bearings and car
				// detection
				mCsvProcessedFile = new CsvFileWriter("Process");
				this.mSavingToProcessedFile = true;
				displayMsg += "\n" + mCsvProcessedFile.getCaptureFileName();

				// 3.If GPS is enabled run the csv file for GPS fixes.
				if (myLocationListener != null) {
					// created the file for saving location information
					mCsvLocationFile = new CsvFileWriter("Location");
					myLocationListener.enableSaveToFile();
					myLocationListener.setCsvFile(mCsvLocationFile);
					displayMsg += "\n" + mCsvLocationFile.getCaptureFileName();
				}

				// 4.update UI
				checkBoxSaveToFile
						.setText(R.string.checkBoxSaveToFileSavingMsg);
				Toast.makeText(this, displayMsg, Toast.LENGTH_SHORT).show();

			} else {
				displayMsg = getString(R.string.checkBoxSaveToFileStoppedMsg);
				// 1.Closing the logging files as it had the same importance as
				// creating them.
				if (mCsvSensorsFile != null) {
					mAccelerationEventListener.disableSaveToFile();
					mCsvSensorsFile.closeCaptureFile();
					displayMsg += "\n" + mCsvSensorsFile.getCaptureFileName();
				}

				// 2.Closing the Processed file.
				this.mSavingToProcessedFile = false;
				mCsvProcessedFile.closeCaptureFile();
				displayMsg += "\n" + mCsvProcessedFile.getCaptureFileName();

				// 3.Closing the location file
				if (mCsvLocationFile != null) {
					myLocationListener.disableSaveToFile();
					mCsvLocationFile.closeCaptureFile();
					displayMsg += "\n" + mCsvLocationFile.getCaptureFileName();
				}

				// 4.update UI
				checkBoxSaveToFile
						.setText(R.string.checkBoxSaveToFileInitialMsg);
				Toast.makeText(this, displayMsg, Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	/**
	 * This handler is used to enable communication with the threads.
	 */
	private final Handler mHandler = new Handler() {
		private int bearingCounter = 0;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.STATE_CONNECTED:
				mCurrentBTState = msg.what;

				// This instance of ConnectedThread is the one that we are going
				// to
				// use write(). We don't need to start the Thread, because we
				// are
				// not going to use read(). [write is not a blocking method].
				BluetoothSocket mSocket = mConnectThread.getBluetoothSocket();
				mConnectedThread = new ConnectedThread(mSocket, mHandler);

				// use sensors
				initiateSensors();

				Toast.makeText(
						getApplicationContext(),
						getString(R.string.title_connected) + " " + mDeviceName,
						Toast.LENGTH_SHORT).show();

				setStatus(getString(R.string.title_connected) + " "
						+ mDeviceName);
				// change the connect icon on the activity.
				if (miSearchOption != null) {
					miSearchOption.setIcon(R.drawable.menu_disconnect_icon);
					miSearchOption.setTitle(R.string.disconnect);
				}
				break;
			case Constants.STATE_CONNECTING:
				/*
				 * TextView tvState = (TextView)
				 * findViewById(R.id.textViewNotConnected);
				 * tvState.setText(R.string.title_connecting);
				 */
				mCurrentBTState = msg.what;
				setStatus(R.string.title_connecting);
				break;
			case Constants.STATE_DISCONNECTED:
				mCurrentBTState = msg.what;
				Toast.makeText(getApplicationContext(),
						getString(R.string.msgUnableToConnect),
						Toast.LENGTH_SHORT).show();
				// initViews();
				miSearchOption.setIcon(R.drawable.menu_connect_icon);
				miSearchOption.setTitle(R.string.connect);
				break;
			case Constants.ACCEL_VALUE_MSG:
				// 1. Receive the linear acceleration values
				earthLinearAccelerationValues = (float[]) msg.obj;

				// 2. calculate the actual acceleration bearing
				elaMovingAverageX.pushValue(earthLinearAccelerationValues[0]);
				elaMovingAverageY.pushValue(earthLinearAccelerationValues[1]);

				mCurrentAccelerationBearing = MathUtil
						.calculateCurrentAccelerationBearing(
								elaMovingAverageY.getMovingAverage(),
								elaMovingAverageX.getMovingAverage());

				// 3.Update the UI (set the value ) as the text of TextViews
				tvXAxisValue.setText(MathUtil.round(
						elaMovingAverageX.getMovingAverage(), 4));
				tvYAxisValue.setText(MathUtil.round(
						elaMovingAverageY.getMovingAverage(), 4));

				// 4. calculate the linear acceleration magnitude. (-z)
				/*
				 * mLinearAccelerationMagnitude = MathUtil
				 * .getVectorMagnitudeMinusZ(earthLinearAccelerationValues);
				 */

				// We're using the average values instead of the direct values.
				mLinearAccelerationMagnitude = MathUtil
						.getVectorMagnitudeMinusZ(
								elaMovingAverageX.getMovingAverage(),
								elaMovingAverageY.getMovingAverage());

				// 5. Detect the situation
				new DisplayDetectedSituationTask().run();

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
					// bearingCounter++;
				}

				// if the counter of getting GPS bearings is more than 5 and not
				// 0 stop the scheduler!
				if (Constants.GPS_MODULE_EXISTS && bearingCounter > 5) {
					// TODO your smart algorithm!
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
	 * Function that is called to start receiving of GPS fix values .
	 */
	private void activateLocationUpdatesFromGPS() {
		if (mLocationManager == null) {
			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			// TODO check this?
			// provider = myLocationManager.getBestProvider(criteria, false);
			// Location loc = myLocationManager
			// .getLastKnownLocation(LocationManager.GPS_PROVIDER);
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					Constants.GPS_MIN_TIME_MIL_SEC,
					Constants.GPS_MIN_DISTANCE_METER, myLocationListener);
		}

	}

	/**
	 * Function that is called to stop receiving of GPS fix values by removing
	 * the listener previously added to location manager.
	 */
	private void deactivateLocationUpdatesFromGPS() {
		// if not null, e.g. GPS was not used in debugging.
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(myLocationListener);
		}
	}

	/**
	 * Show weather a brake or acceleration has occurred, it considers if the
	 * acceleration magnitude on y (North) and x (East) is more than a certain
	 * threshold and if it's been going on for more than a certain time.
	 * 
	 * Later on it starts to log values if saving to file has been enabled.
	 * 
	 * @author Vahid
	 *
	 */
	private final class DisplayDetectedSituationTask implements Runnable {
		@Override
		public void run() {
			// 5. updating the UI with Acceleration Magnitude
			tvFinalValue.setText(MathUtil
					.round(mLinearAccelerationMagnitude, 3));
			int progressPercentage = (int) (mLinearAccelerationMagnitude * 5);

			float bearingDifference = MathUtil.getBearingsAbsoluteDifference(
					mCurrentAccelerationBearing, mCurrentMovementBearing);
			// update UI, for debugging.
			// check if the values are more than threshold
			if (mLinearAccelerationMagnitude >= Constants.ACCEL_THRESHOLD) {
				if (bearingDifference > Constants.DIFF_DEGREE_BRAKE) {
					mAccelSituation = Constants.BRAKE_DETECTED;
					// mBackground.setBackgroundResource(R.color.dark_red);

					// TODO: To make sure if it's a brake.
					decelerationMovingAverageTime.pushValue(
							(float) mLinearAccelerationMagnitude, new Date());
					// so it seems they're different?
					mAccelProgressBar.setProgress(0);
					mBrakeProgressBar.setProgress(progressPercentage);

					// sending the brake intensity to BT module-Arduino
					if (Constants.BT_MODULE_EXISTS) {
						writeToBluetoothDevice(mLinearAccelerationMagnitude);
					}
				} else {
					// Very smart, if the degree is more than path_change(40)
					// and less than brake (90) this is most prob. a direction
					// change.
					/*
					 * if (Constants.GPS_MODULE_EXISTS && bearingDifference >=
					 * Constants.DIFF_DEGREE_PATH_CHANGE) {
					 * activateLocationUpdatesFromGPS(); }
					 */
					mAccelSituation = Constants.ACCEL_DETECTED;
					// mBackground.setBackgroundResource(R.color.dark_green);
					decelerationMovingAverageTime.clearValues();

					// so it seems they're different?
					if (Constants.BT_MODULE_EXISTS) {
						writeToBluetoothDevice(0);
					}
					mBrakeProgressBar.setProgress(0);

					mAccelProgressBar.setProgress(progressPercentage);

				}
			} else {
				mAccelSituation = Constants.NO_MOVE_DETECTED;
				decelerationMovingAverageTime.clearValues();

				if (Constants.BT_MODULE_EXISTS) {
					writeToBluetoothDevice(0);
				}
				mBrakeProgressBar.setProgress(0);
				mAccelProgressBar.setProgress(0);
			}

			// write values to file.
			if (mSavingToProcessedFile) {
				// saving current time
				Date date = new Date();
				mCsvProcessedFile.writeToFile(Long.toString(date.getTime()),
						false);

				mCsvProcessedFile.writeToFile(mCurrentAccelerationBearing,
						false);
				mCsvProcessedFile.writeToFile(mCurrentMovementBearing, false);
				mCsvProcessedFile.writeToFile(
						(float) mLinearAccelerationMagnitude, false);
				mCsvProcessedFile.writeToFile(mAccelSituation, true);
			}

		}

	}

	/**
	 * Method that is called when sending the byte magnitude values to the
	 * Bluetooth connected module.
	 * 
	 * @param magnitude
	 */
	private void writeToBluetoothDevice(double magnitude) {
		// calculate light intensity to be used for wpm display of light amount
		int lightIntensity = (int) (magnitude * 255 / Constants.MAX_ACCEL);
		byte[] resultBytes = MathUtil.intToByteArray(lightIntensity);

		/*
		 * byte[] resultBytes = MathUtil.doubleToByteArray(magnitude); float
		 * tempMagnitude = (float) magnitude; byte[] resultBytes2 =
		 * MathUtil.floatToByteArray(tempMagnitude);
		 */
		mConnectedThread.write(resultBytes[3]);
	}

	/**
	 * Dialog that is displayed when no bluetooth is found on the device. The
	 * app then closes.
	 */
	private void noBluetoothDetected() {
		// TODO
		/*
		 * btnConnectBT.setVisibility(View.GONE);
		 * tvState.setText("Device does not support Bluetooth");
		 */
		Toast.makeText(getApplicationContext(), R.id.imageViewWrong,
				Toast.LENGTH_SHORT).show();
		// ImageView ivError = (ImageView) findViewById(R.id.imageViewWrong);
		// ivError.setVisibility(View.VISIBLE);
	}

	/**
	 * Dialog that asks from the user to enable the bluetooth.
	 */
	private void enableBluetoothDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				ConnectedBarsActivity.this);
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
						Toast.makeText(ConnectedBarsActivity.this,
								R.string.bt_required, Toast.LENGTH_SHORT)
								.show();
					}
				});
		alertDialog.create();
		alertDialog.show();
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

		// mDeviceName = device.getName();
		// The receiver (mReceiver) is waiting for the signal of
		// "device connected" to use the connection, and call connected().
		// connected() initialize also the ConnectedThread instance
		// (- connected = new ConnectedThread(BleutoothSocket) -)
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
			// search for devices.
			runBluetoothDevicesActivity();
		}

		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

					if (intent.getIntExtra(
							BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned off", Toast.LENGTH_SHORT)
								.show();
						initiateViews();
					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned ON", Toast.LENGTH_SHORT)
								.show();
						initiateViews();
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
		registerReceiver(mBroadcastReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED); //
		registerReceiver(mBroadcastReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		registerReceiver(mBroadcastReceiver, filter);
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
}
