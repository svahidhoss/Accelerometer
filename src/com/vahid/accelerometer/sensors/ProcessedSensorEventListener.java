package com.vahid.accelerometer.sensors;

import java.util.Arrays;
import java.util.Date;

import com.vahid.accelerometer.util.MathUtil;
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

public class ProcessedSensorEventListener implements
		SensorEventListenerInterface, CsvListenerInterface {

	private Handler mHandler;

	/**** Sensor related Fields ****/
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mOrientation, mLinearAcceleration, mGravity,
			mMagneticField;
	// Sensor Values: it's important to initialize them.
	// private float[] mAcceleromterValues = new float[] { 0, 0, 0 };
	// private float[] orientationValues = new float[] { 0, 0, 0 };
	private float[] mLinearAccelerationValues = new float[] { 0, 0, 0 };
	private float[] mMagneticValues;
	private float[] mGravityValues;

	private double linearAccelerationMagnitude;

	// Rotation Matrix Calculation
	private float[] mEarthLinearAccelerationValues = new float[4];
	private float[] inclinationMatrix = new float[16];
	private float[] rotationMatrix = new float[16];
	private float[] rotationMatrixInverse = new float[16];

	private double trueAccelerationMagnitude;
	private float magneticBearing;

	// save to file view fields
	private boolean savingToFile = false;
	private CsvFileWriter mCsvFile;

	public ProcessedSensorEventListener(Handler mHandler) {
		this.mHandler = mHandler;
	}

	@Override
	public void initializeSensors(SensorManager mSensorManager) {
		this.mSensorManager = mSensorManager;

		// mAccelerometer = mSensorManager
		// .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// it is deprecated, but it works.
		// mOrientation =
		// mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		mLinearAcceleration = mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mMagneticField = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "Sensors initialized");

	}

	@Override
	public void registerSensors(int curDelayRate) {
		mSensorManager.unregisterListener(this);

		mSensorManager.registerListener(this, mMagneticField,
				Constants.DELAY_RATES[curDelayRate]);
		mSensorManager.registerListener(this, mGravity,
				Constants.DELAY_RATES[curDelayRate]);

		mSensorManager.registerListener(this, mLinearAcceleration,
				Constants.DELAY_RATES[curDelayRate]);

		if (Constants.DEBUG)
			Log.d(Constants.LOG_TAG, "sensors registered");
	}

	@Override
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
	}

	private void getLinearAcceleration(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
			mLinearAccelerationValues = event.values;

		if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
			mGravityValues = event.values;

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mMagneticValues = event.values;

		if (mGravityValues != null && mMagneticValues != null) {
			boolean allGood = SensorManager.getRotationMatrix(rotationMatrix,
					inclinationMatrix, mGravityValues, mMagneticValues);

			if (allGood && mLinearAccelerationValues != null) {
				/*
				 * float orientation[] = new float[3];
				 * SensorManager.getOrientation(R, orientation);
				 */

				float[] linearAccelerationValuesNew = {
						mLinearAccelerationValues[0],
						mLinearAccelerationValues[1],
						mLinearAccelerationValues[2], 0 };

				Matrix.invertM(rotationMatrixInverse, 0, rotationMatrix, 0);
				Matrix.multiplyMV(mEarthLinearAccelerationValues, 0,
						rotationMatrixInverse, 0, linearAccelerationValuesNew,
						0);

				// TODO testing the orientation
				// SensorManager.remapCoordinateSystem(rotationMatrix,
				// SensorManager.AXIS_X, SensorManager.AXIS_Y,
				// outRotationMatrix);
				// SensorManager.getOrientation(outRotationMatrix,
				// orientationValuesRadian);
				//
				// orientationValuesDegrees[0] = (float)
				// Math.toDegrees(orientationValuesRadian[0]);
				// orientationValuesDegrees[1] = (float)
				// Math.toDegrees(orientationValuesRadian[1]);
				// orientationValuesDegrees[2] = (float)
				// Math.toDegrees(orientationValuesRadian[2]);

				// This is Magnetic North in Radians
				// bearing = orientation[0];

				// ATTENTION: The Bearing will change based on Screen
				// Orientation!!!
				// Tilt your device and see what I mean ...see?
				// Outside the scope of this sample, but easy to compensated
				// for.

				// Convert to degrees & 360 span (feel like I'm in Trig class
				// with Radians)
				// magneticBearing = ((float)
				// Math.toDegrees(orientationValuesRadian[0]) + 360) % 360;

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
				linearAccelerationMagnitude = MathUtil
						.getVectorMagnitude(mLinearAccelerationValues);
				trueAccelerationMagnitude = MathUtil
						.getVectorMagnitude(mEarthLinearAccelerationValues);

				// If check box for saving the file has been checked.
				if (savingToFile && mCsvFile != null) {
					// current time stamp
					Date date = new Date();
					mCsvFile.writeToFile(Long.toString(date.getTime()), false);

					mCsvFile.writeToFile(mLinearAccelerationValues, false);
					// write the values of the true acceleration
					mCsvFile.writeToFile(Arrays.copyOfRange(
							mEarthLinearAccelerationValues, 0,
							mEarthLinearAccelerationValues.length - 1), false);
					mCsvFile.writeToFile((float) linearAccelerationMagnitude,
							false);
					mCsvFile.writeToFile((float) trueAccelerationMagnitude,
							false);
					// mCsvFile.writeToFile(orientationValuesDegrees, false);
					mCsvFile.writeToFile(rotationMatrix, true);
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
						mEarthLinearAccelerationValues).sendToTarget();
				/*
				 * mHandler.obtainMessage(Constants.MAGNETIC_BEARING_MSG,
				 * magneticBearing).sendToTarget();
				 */

			}
		}
	}

	private void getMagneticField(SensorEvent event) {
		mMagneticValues = event.values;
	}

	private void getGravity(SensorEvent event) {
		mGravityValues = event.values;
	}

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
		this.mCsvFile = csvFile;
		String names[] = { "Time", "LinearAcceleration - X",
				"LinearAcceleration - Y", "LinearAcceleration - Z",
				"EarthLinearAcceleration - X", "EarthLinearAcceleration - Y",
				"EarthLinearAcceleration - Z",
				"Linear Acceleration Magnitude(phone)",
				"Linear Acceleration Magnitude(earth)", "rotation matrix[0 0]",
				"rotation matrix[0 1]", "rotation matrix[0 2]",
				"rotation matrix[0 3]", "rotation matrix[1 0]",
				"rotation matrix[1 1]", "rotation matrix[1 2]",
				"rotation matrix[1 3]", "rotation matrix[2 0]",
				"rotation matrix[2 1]", "rotation matrix[2 2]",
				"rotation matrix[2 3]", "rotation matrix[3 0]",
				"rotation matrix[3 1]", "rotation matrix[3 2]",
				"rotation matrix[3 3]" };
		mCsvFile.writeFileTitles(names);
	}

}
