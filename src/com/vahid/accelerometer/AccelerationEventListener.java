package com.vahid.accelerometer;

import java.util.Arrays;

import com.vahid.accelerometer.util.AlexMath;
import com.vahid.accelerometer.util.CsvListenerInterface;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

public class AccelerationEventListener implements SensorEventListener,
		CsvListenerInterface {

	private Handler mHandler;
	private static final int delayRates[] = {
			SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI,
			SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST };

	/**** Sensor related Fields ****/
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mOrientation, mLinearAcceleration, mGravity,
			mMagneticField;
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
	
	// TODO testing the orientation values
	private float[] orientationValuesRadian = new float[] { 0, 0, 0 };
	private float[] orientationValuesDegrees = new float[] { 0, 0, 0 };
	private float[] outRotationMatrix = new float[16];

	private double trueAccelerationMagnitude;
	private float magneticBearing;


	// save to file view fields
	private boolean savingToFile = false;
	private CsvFileWriter csvFile;

	public AccelerationEventListener(Handler mHandler) {
		this.mHandler = mHandler;
	}

	/**
	 * Method that initializes the sensors, after we're connected to the server.
	 * 
	 * @param mSensorManager
	 */
	public void initializeSensors(SensorManager mSensorManager) {
		this.mSensorManager = mSensorManager;

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
	public void registerSensors(int curDelayRate) {
		mSensorManager.unregisterListener(this);

		// TODO change this
		// mSensorManager.registerListener(this, mAccelerometer,
		// delayRates[curDelayRate]);

		// mSensorManager.registerListener(this, mOrientation,
		// delayRates[curDelayRate]);

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
	public void unregisterSensors() {
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	// Don't block the onSensorChanged() method
	@Override
	public void onSensorChanged(SensorEvent event) {
		// synchronized (this) {
		switch (event.sensor.getType()) {
		// TODO change this
		// case Sensor.TYPE_ACCELEROMETER:
		// // getAccelerometer(event);
		// break;
		// case Sensor.TYPE_ORIENTATION:
		// // getOrientation(event);
		// break;
		case Sensor.TYPE_LINEAR_ACCELERATION:
			getLinearAcceleration(event);
			// getLinearAcceleration(event);
			// getAccelerometer(event);

			break;
		case Sensor.TYPE_GRAVITY:
			getLinearAcceleration(event);
			// getGravity(event);

			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			getLinearAcceleration(event);
			// getMagneticField(event);

			break;
		default:
			break;
		}

		// } //fin del syncronized this. //#check?
	}

	private void getLinearAcceleration(SensorEvent event) {
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
				
				// TODO testing the orientation
				SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, outRotationMatrix);
				SensorManager.getOrientation(outRotationMatrix, orientationValuesRadian);
				
				orientationValuesDegrees[0] = (float) Math.toDegrees(orientationValuesRadian[0]);
				orientationValuesDegrees[1] = (float) Math.toDegrees(orientationValuesRadian[1]);
				orientationValuesDegrees[2] = (float) Math.toDegrees(orientationValuesRadian[2]);

				// This is Magnetic North in Radians
				// bearing = orientation[0];

				// ATTENTION: The Bearing will change based on Screen
				// Orientation!!!
				// Tilt your device and see what I mean ...see?
				// Outside the scope of this sample, but easy to compensated
				// for.

				// Convert to degrees & 360 span (feel like I'm in Trig class
				// with Radians)
				magneticBearing = ((float) Math.toDegrees(orientationValuesRadian[0]) + 360) % 360;

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

				// If check box for saving the file has been checked.
				if (savingToFile && csvFile != null) {
					// write the values of the linear acceleration
					csvFile.writeToFile(linearAccelerationValues, false);
					csvFile.writeToFile((float) linearAccelerationMagnitude,
							false);
					// write the values of the true acceleration
					csvFile.writeToFile(Arrays.copyOfRange(trueAcceleration, 0,
							trueAcceleration.length - 1), false);
					csvFile.writeToFile((float) trueAccelerationMagnitude, false);
					csvFile.writeToFile(orientationValuesDegrees, false);
					csvFile.writeToFile(rotationMatrix, true);
				}

				/*
				 * tvXAxisValue.setText(Float.toString(trueAcceleration[0]));
				 * tvYAxisValue.setText(Float.toString(trueAcceleration[1]));
				 * tvZAxisValue.setText(Float.toString(trueAcceleration[2]));
				 * tvFinalValue
				 * .setText((AlexMath.round(trueAccelerationMagnitude, 10)));
				 * 
				 * // set the value on to the SeekBar
				 * xAxisSeekBar.setProgress((int) (trueAcceleration[0] + 10f));
				 * yAxisSeekBar.setProgress((int) (trueAcceleration[1] + 10f));
				 * zAxisSeekBar.setProgress((int) (trueAcceleration[2] + 10f));
				 * 
				 * finalSeekBar .setProgress((int) (trueAccelerationMagnitude +
				 * 10f));
				 */

				mHandler.obtainMessage(Constants.ACCEL_VALUE_MSG,
						trueAcceleration).sendToTarget();
				mHandler.obtainMessage(Constants.MAGNETIC_BEARING_MSG,
						magneticBearing).sendToTarget();

			}
		}
	}

	private void getMagneticField(SensorEvent event) {
		magneticValues = event.values;
	}

	private void getGravity(SensorEvent event) {
		gravityValues = event.values;
	}

	/*
	 * private void getAccelerometer(SensorEvent event) { acceleromterValues =
	 * event.values;
	 * 
	 * // not needed if linear acceleration is used. // acceleromterValues =
	 * AlexMath.cancelGravity(acceleromterValues, // orientationValues); // the
	 * sensor doesn't erase the // gravity by itself // for that exists other
	 * sensor: // LINEAR_ACCELERATION, but is not // very typical to have it.
	 * 
	 * 
	 * we don't need to cancel the gravity, because we are going to use the axes
	 * x0,y0,z0, in which angles the gravity are all it in z0 // but we do...
	 * the results appears to be better.
	 * 
	 * acceleromterValues = AlexMath.convertReference(acceleromterValues,
	 * orientationValues);
	 * 
	 * linearAccelerationMagnitude = AlexMath
	 * .getVectorMagnitude(acceleromterValues);
	 * 
	 * // *******first filter of braking.
	 * 
	 * // *********braking????********* boolean braking = false; if
	 * (acceleromterValues[1] > 0 && Math.abs(orientationValues[1]) < 90 +
	 * Constants.pitchPrecision) { braking = true; } else if
	 * (acceleromterValues[1] < 0 && Math.abs(orientationValues[1]) > 90 +
	 * Constants.pitchPrecision) { braking = true; }
	 * 
	 * boolean accelerating = false; if (acceleromterValues[1] < 0 &&
	 * Math.abs(orientationValues[1]) < 90) { accelerating = true; } else if
	 * (acceleromterValues[1] > 0 && Math.abs(orientationValues[1]) > 90) {
	 * accelerating = true; } // ******end***braking?????****
	 * 
	 * // Braking is the boolean variable that defines the brake and the //
	 * acceleration with the sign of y if (linearAccelerationMagnitude >
	 * Constants.accelerationPrecision && braking) { RelativeLayout background =
	 * (RelativeLayout) findViewById(R.id.activity_main_connected);
	 * background.setBackgroundResource(R.color.dark_red);
	 * 
	 * if (breakOn == false) { breakOn = true; breakReal = false;
	 * breakInitializedTime = Calendar.getInstance();
	 * 
	 * } else { if (Calendar.getInstance().getTimeInMillis() -
	 * breakInitializedTime.getTimeInMillis() > Constants.marginMilliseconds) {
	 * breakReal = true; try { // connected.write((int) (10*module)); //
	 * connected.write(mmath.toByteArray(module));
	 * 
	 * } catch (Exception e) { Toast.makeText(getApplicationContext(),
	 * "error writting", Toast.LENGTH_SHORT).show(); } TextView tvaux =
	 * (TextView) findViewById(R.id.textViewMain); tvaux.setText("" +
	 * AlexMath.round(linearAccelerationMagnitude, 1)); ProgressBar progress =
	 * (ProgressBar) findViewById(R.id.seekBar1); progress.setProgress((int)
	 * linearAccelerationMagnitude);
	 * 
	 * } else { breakReal = false; }
	 * 
	 * }
	 * 
	 * }// end break starts else { // ***** TextView tvaux = (TextView)
	 * findViewById(R.id.textViewMain); tvaux.setText(""); // -- ProgressBar
	 * progress = (ProgressBar) findViewById(R.id.seekBar1);
	 * progress.setProgress((int) (0)); // ***** breakReal = false; breakOn =
	 * false; RelativeLayout backg = (RelativeLayout)
	 * findViewById(R.id.activity_main_connected); // if
	 * (acceleromterValues[1]<0 && // module>Constant.precision){ if
	 * (accelerating && linearAccelerationMagnitude >
	 * Constants.accelerationPrecision) {
	 * backg.setBackgroundResource(R.color.dark_green); } else {
	 * backg.setBackgroundColor(Color.WHITE);
	 * 
	 * }
	 * 
	 * }// end 'end brake'
	 * 
	 * // ******end the first filter to brakings
	 *//**
	 * ******WRITE TO DE BLUETOOTH DEVICE*********
	 */
	/*
	 * if (Constants.BT_MODULE_EXISTS) {
	 * writeToBluetoothDevice(linearAccelerationMagnitude); }
	 * 
	 * }
	 */

	/*
	 * private void getOrientation(SensorEvent event) { if (Constants.DEBUG)
	 * Log.d(Constants.LOG_TAG, "copy Orientation");
	 * 
	 * // see the values in phone screen (debug) String angles = "azimuth: " +
	 * AlexMath.round(event.values[0], 3) + "\npitch:     " +
	 * AlexMath.round(event.values[1], 3) + "\nroll:       " +
	 * AlexMath.round(event.values[2], 3); TextView tv = (TextView)
	 * findViewById(R.id.textViewConnected); tv.setText(angles);
	 * 
	 * if (onAngles == false) { orientationValues = event.values.clone();
	 * orientationValuesEarlier = event.values.clone(); onAngles = true; }
	 * orientationValues[0] = event.values[0];
	 * 
	 * // ****calculate angles average
	 * 
	 * float delta1 = event.values[1] - orientationValuesEarlier[1]; float
	 * delta2 = event.values[2] - orientationValuesEarlier[2]; float delta =
	 * Math.max(delta1, delta2);
	 * 
	 * // .1 if (delta < Constants.delta && noise == false) { sum_angles1 =
	 * sum_angles1 + event.values[1]; sum_angles2 = sum_angles2 +
	 * event.values[2]; n++; orientationValues[1] = (float) sum_angles1 / n;
	 * orientationValues[2] = (float) sum_angles2 / n; if (n == Constants.nIter)
	 * {
	 * 
	 * sum_angles1 = event.values[1]; sum_angles2 = event.values[2]; n = 1; } }
	 * else if (delta > Constants.delta) {
	 * 
	 * noise = true; sum_angles1_aux = 0; n = 0; } else if (delta <
	 * Constants.delta && noise == true) { sum_angles1_aux = sum_angles1_aux +
	 * event.values[1]; sum_angles2_aux = sum_angles2_aux + event.values[2];
	 * n++; if (n == Constants.nIter) { orientationValues[1] = (float)
	 * sum_angles1_aux / n; orientationValues[2] = (float) sum_angles2_aux / n;
	 * 
	 * sum_angles1 = event.values[1]; sum_angles2 = event.values[2]; n = 1;
	 * noise = false; } }
	 * 
	 * // .2 // ****end***calculate angles average orientationValuesEarlier =
	 * event.values.clone(); }// end case sensor Orientation
	 */
	@Override
	public void enableSaveToFile() {
		this.savingToFile = true;
	}

	@Override
	public void disableSaveToFile() {
		this.savingToFile = false;
	}

	@Override
	public void setCsvFile(CsvFileWriter csvFile) {
		this.csvFile = csvFile;
	}

}
