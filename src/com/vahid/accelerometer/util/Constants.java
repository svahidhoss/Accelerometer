package com.vahid.accelerometer.util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.hardware.SensorManager;

public class Constants {
	/** Debug related constants */
	public static boolean DEBUG = true;
	public static String LOG_TAG = "Debugging Tag";
	// Used in cases of no BT module
	public static boolean BT_MODULE_EXISTS = false;
	public static boolean GPS_MODULE_EXISTS = false;
	
	
	/** General Variables **/
	public static final int MAC_ADDRESS_CHAR_LENGTH = 17;

	/** GPS values **/
	public static long GPS_MIN_TIME_MIL_SEC = TimeUnit.SECONDS.toMillis(5);
	public static float GPS_MIN_DISTANCE_METER = 5;

	/** Brake detection **/
	// low pass filter: moving average - app 4 Seconds
	public static int WINDOW_SIZE = 20;
	public static long WINDOW_SIZE_IN_MILI_SEC = TimeUnit.SECONDS
			.toMillis(1);
	public static long RUNNING_PERIOD = TimeUnit.MILLISECONDS
			.toMillis(1);
	// The brake threshold based on heuristic 1 m/s2
	public static float ACCEL_THRESHOLD = 1f;
	public static float BRAKE_THRESHOLD = -ACCEL_THRESHOLD;
	
	// difference in degree values
	public static final float DIFF_DEGREE = 90;
	
	// Three situations of brake condition
	public static final int BRAKE_DETECTED = -1;
	public static final int NO_MOVE_DETECTED = 0;
	public static final int ACCEL_DETECTED = 1;

	
	/** Sensor Accelerometer Rates **/
	public static final int DELAY_RATES[] = {
			SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI,
			SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST };
	public static final String DELAY_RATES_DESCRIPTION[] = {
			"Normal 200,000 ms", "UI 60,000 ms", "Game 20,000 ms",
			"Fastest 0 ms" };

	/**** Bluetooth related fields ****/
	// MY_UUID is the app's UUID string, also used by the server code?
	public static final UUID MY_UUID = UUID
			.fromString("04c6032b-0000-4000-8000-00805f9b34fc");
	// public static final UUID MY_UUID = UUID
	// .fromString("00001105-0000-1000-8000-00805F9B34FB");

	/**** Handler Section ****/
	// Constants that indicate the current connection state
	// we're doing nothing
	public static final int STATE_DISCONNECTED = 0;
	// initiating an outgoing connection
	public static final int STATE_CONNECTING = 1;
	// connected to a remote device
	public static final int STATE_CONNECTED = 2;
	// Sending back calculated values
	public static final int ACCEL_VALUE_MSG = 3;
	public static final int MAGNETIC_BEARING_MSG = 4;
	public static final int MOVEMENT_BEARING_MSG = 5;
	public static final int DECLINATION_MSG = 6;
	
	
	/**** Settings related constants ****/
	public static long marginMilliseconds = 300;
	public static float accelerationPrecision = (float) 0.9;
	public static float pitchPrecision = 0f; // pitch defines design of the
												// acceleration (if it is
												// braking or accelerating) (=0
												// by the moment)
	
	// ****calculate angles average
	public static int delta = 30; // acceptable error in each angle.
	public static int nIter = 10; // number of iteration to actualize the
									// average angle (1 iteration = 0.1 seconds,
									// aprox).
	// *****end*angles average

}
