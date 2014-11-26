package com.vahid.accelerometer;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

import com.vahid.accelerometer.bluetooth.ConnectedThread;
import com.vahid.accelerometer.filter.MovingAverage;
import com.vahid.accelerometer.filter.MovingAverage2;
import com.vahid.accelerometer.filter.MovingAverageTime;
import com.vahid.accelerometer.sensors.AccelerationEventListener;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.accelerometer.util.MathUtil;
import com.vahid.acceleromter.location.MyLocationListener;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ConnectedDebugActivity extends Activity {

	/**** Defining view fields ****/
	private LinearLayout mBackground;
	// 2. Connected views
	// private SeekBar xAxisSeekBar, yAxisSeekBar, zAxisSeekBar;
	private ProgressBar mFinalProgressBar;
	private TextView tvXAxisValue, tvYAxisValue, tvZAxisValue, tvFinalValue;
	private TextView tvRotationDegreeValue, tvAccelerationDegreeValue,
			tvDifferenceDegreeeValue, tvBrake, tvBrakeValue;
	/* the Spinner component for delay rate */
	private Spinner delayRateChooser;
	private CheckBox checkBoxSaveToFile;

	/**** Save to file view fields ****/
	private CsvFileWriter mCsvSensorsFile, mCsvLocationFile, mCsvProcessedFile;
	// boolean to check if it should save to mCsvProcessedFile;
	private boolean mSavingToProcessedFile = false;
	// The Runnable task to detect if there's a brake and save to file.
	// private Runnable mDisplayDetectedSituationTask;

	/**** Location Related fields ****/
	private MyLocationListener myLocationListener;
	private LocationManager mLocationManager;
	private ScheduledExecutorService mGpsExecutor;

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
	private MovingAverage2 elaMovingAverageX, elaMovingAverageY,
			elaMovingAverageZ;
	private MovingAverage laMagMovingAverage;
	private MovingAverage mCurAccBearingMovingAverage,
			mCurMovBearingMovingAverage;

	// Used for loading values in this list to execute the average deceleration
	// after it exceeds the certain threshold. It's cleaned after an
	// acceleration again.
	private MovingAverageTime decelerationMovingAverageTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initViewsConnectedLinearAcceleration();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the connected_menu; this adds items to the action bar if it
		// is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
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
		// when information comes manually from the settings activity. (when GPS
		// is deactivated).
		case Constants.REQUEST_SETTINGS_CHANGE:
			if (resultCode == RESULT_OK) {
				mCurrentMovementBearing = Math.abs(data.getExtras().getFloat(
						SettingsActivity.SET_BEARING));
			}
			break;
		// TODO no bt for now.
		/*
		 * case Constants.REQUEST_ENABLE_BT: if (resultCode == RESULT_CANCELED)
		 * { Toast.makeText(this, R.string.bt_required, Toast.LENGTH_SHORT)
		 * .show(); } if (resultCode == RESULT_OK) { Toast.makeText(this,
		 * "Bluetooth is enabled.", Toast.LENGTH_SHORT).show(); if
		 * (Constants.BT_MODULE_EXISTS) { runBluetoothDevicesActivity(); } }
		 * break; case Constants.REQUEST_CONNECT_DEVICE: if (resultCode ==
		 * RESULT_OK) { String passedAddress = data.getExtras().getString(
		 * BluetoothDevicesActivity.EXTRA_ADDRESS);
		 * 
		 * connectBluetoothDevice(passedAddress); } else {
		 * initViewsNotConnected(); } break;
		 */
		default:
			break;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (Constants.GPS_MODULE_EXISTS) {
			// shutdown the GPS Executor immediately
			if (mGpsExecutor != null) {
				mGpsExecutor.shutdown();
			}

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
	private void initViewsConnectedLinearAcceleration() {
		// TODO enable after BT
		/*
		 * if (Constants.BT_MODULE_EXISTS) {
		 * Toast.makeText(getApplicationContext(),
		 * getString(R.string.title_connected) + mDeviceName,
		 * Toast.LENGTH_SHORT).show(); }
		 */

		setContentView(R.layout.activity_connected_debug);
		// initiate moving averages
		initiateMovingAverages();

		// This instance of ConnectedThread is the one that we are going to
		// use write(). We don't need to start the Thread, because we are not
		// going to use read(). [write is not a blocking method].
		// TODO
		/*
		 * if (Constants.BT_MODULE_EXISTS) { BluetoothSocket mSocket =
		 * mConnectThread.getBluetoothSocket(); mConnectedThread = new
		 * ConnectedThread(mSocket, mHandler);
		 * 
		 * }
		 */

		// we are ready for GPS, Only used when we have the GPS.
		if (Constants.GPS_MODULE_EXISTS) {
			activateLocationUpdatesFromGPS();
		}

		// we are also ready to use the sensor and send the information of the
		// brakes, so...
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerationEventListener = new AccelerationEventListener(mHandler);
		mAccelerationEventListener.initializeSensors(mSensorManager);
		mAccelerationEventListener.registerSensors(mCurrentDelayRate);

		// retrieve all the needed components
		mBackground = (LinearLayout) findViewById(R.id.activity_main_las);

		tvXAxisValue = (TextView) findViewById(R.id.xAxisValue);
		tvYAxisValue = (TextView) findViewById(R.id.yAxisValue);
		tvZAxisValue = (TextView) findViewById(R.id.zAxisValue);

		tvFinalValue = (TextView) findViewById(R.id.finalValue);
		mFinalProgressBar = (ProgressBar) findViewById(R.id.finalProgressBar);

		tvRotationDegreeValue = (TextView) findViewById(R.id.rotationDegreeeValue);
		tvAccelerationDegreeValue = (TextView) findViewById(R.id.accelerationDegreeeValue);
		tvDifferenceDegreeeValue = (TextView) findViewById(R.id.differenceDegreeeValue);

		tvBrake = (TextView) findViewById(R.id.brakeTextView);
		tvBrakeValue = (TextView) findViewById(R.id.brakeValueTextView);

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
		elaMovingAverageZ = new MovingAverage2(
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
			case Constants.ACCEL_VALUE_MSG:
				// 1. Receive the linear acceleration values
				earthLinearAccelerationValues = (float[]) msg.obj;

				// 2. calculate the actual acceleration bearing
				elaMovingAverageX.pushValue(earthLinearAccelerationValues[0]);
				elaMovingAverageY.pushValue(earthLinearAccelerationValues[1]);
				elaMovingAverageZ.pushValue(earthLinearAccelerationValues[2]);

				mCurrentAccelerationBearing = MathUtil
						.calculateCurrentAccelerationBearing(
								elaMovingAverageY.getMovingAverage(),
								elaMovingAverageX.getMovingAverage());

				// 3.Update the UI (set the value ) as the text of every
				// TextView
				tvXAxisValue.setText(MathUtil.round(
						elaMovingAverageX.getMovingAverage(), 6));
				tvYAxisValue.setText(MathUtil.round(
						elaMovingAverageY.getMovingAverage(), 6));
				tvZAxisValue.setText(MathUtil.round(
						elaMovingAverageZ.getMovingAverage(), 6));

				// 4. calculate the linear acceleration mag. (-z)
				mLinearAccelerationMagnitude = MathUtil
						.getVectorMagnitudeMinusZ(elaMovingAverageY.getMovingAverage(),
								elaMovingAverageX.getMovingAverage());
				// laMagMovingAverage
				// .pushValue((float) mLinearAccelerationMagnitude);

				// 5. updating the UI with Acceleration Magnitude
				tvFinalValue.setText(MathUtil.round(
						mLinearAccelerationMagnitude, 3));
				int progressPercentage = (int) (mLinearAccelerationMagnitude * 5);
				mFinalProgressBar.setProgress(progressPercentage);

				// 6. Detect the situation
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
					bearingCounter++;
				}

				// if the counter of getting GPS bearings is more than 5 and not
				// 0
				// stop the scheduler!
				if (Constants.GPS_MODULE_EXISTS && bearingCounter > 5) {
					mGpsExecutor.shutdown();
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
		RequestLocationGPSUpdatesTask reqLocUpdatesFromGPSTask = new RequestLocationGPSUpdatesTask();
		reqLocUpdatesFromGPSTask.run();
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

	private final class RequestLocationGPSUpdatesTask implements Runnable {
		@Override
		public void run() {
			myLocationListener = new MyLocationListener(
					getApplicationContext(), mHandler);
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
	 * Show weather a brake or acceleration has occurred, it considers if the
	 * acceleration magnitude on y (North) and x (East) is more than a certain
	 * threshold and if it's been going on for more than a certain time.
	 * 
	 * Later on it decides to logs the values if saving to file has been
	 * enabled.
	 * 
	 * @author Vahid
	 *
	 */
	private final class DisplayDetectedSituationTask implements Runnable {

		@Override
		public void run() {
			float bearingDifference = MathUtil.getBearingsAbsoluteDifference(
					mCurrentAccelerationBearing, mCurrentMovementBearing);
			// update UI, for debugging.
			tvDifferenceDegreeeValue.setText(Float.toString(bearingDifference));
			// check if the values are more than threshold
			if (mLinearAccelerationMagnitude >= Constants.ACCEL_THRESHOLD) {
				if (bearingDifference > Constants.DIFF_DEGREE_BRAKE) {
					mAccelSituation = Constants.BRAKE_DETECTED;
					mBackground.setBackgroundResource(R.color.dark_red);

					// TODO: To make sure if it's a brake.
					decelerationMovingAverageTime.pushValue(
							(float) mLinearAccelerationMagnitude, new Date());

					/*
					 * mFinalProgressBar.setProgressDrawable(getResources().
					 * getDrawable ( R.drawable.progress_bar_vahid_red));
					 */
				} else {
					// Very smart, if the degree is more than path_change(40)
					// and less than brake (90) this is most prob. a direction
					// change.
					if (Constants.GPS_MODULE_EXISTS && bearingDifference >= Constants.DIFF_DEGREE_PATH_CHANGE) {
						activateLocationUpdatesFromGPS();
					}
					mAccelSituation = Constants.ACCEL_DETECTED;
					mBackground.setBackgroundResource(R.color.dark_green);
					decelerationMovingAverageTime.clearValues();
					/*
					 * mFinalProgressBar.setProgressDrawable(getResources().
					 * getDrawable ( R.drawable.progress_bar_vahid_green));
					 */
				}
			} else {
				mAccelSituation = Constants.NO_MOVE_DETECTED;
				mBackground.setBackgroundResource(R.color.White);
				decelerationMovingAverageTime.clearValues();
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
}
