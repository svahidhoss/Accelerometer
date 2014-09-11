package com.vahid.accelerometer.util;

import java.util.UUID;

public class Constants {
	/** Brake detection **/
	// low pass filter: moving average - app 4 Seconds
	public static final int WINDOW_SIZE = 20;
	// The brake threshold based on heuristic 1 m/s2
	public static final float BRAKE_THRESHOLD = -0.75f;
	public static final float ACCEL_THRESHOLD = -BRAKE_THRESHOLD;
	// Three situations for brake condition
	public static final int BRAKE_DETECTED = -1;
	public static final int NO_MOVE_DETECTED = 0;
	public static final int ACCEL_DETECTED = 1;

	// Debug related constants
	public static boolean DEBUG = true;
	public static String LOG_TAG = "Debugging Tag";
	// Used in cases of no BT module
	public static boolean BT_MODULE_EXISTS = false;

	public static final int MAC_ADDRESS_CHAR_LENGTH = 17;

	/**** Settings related constants ****/
	public static long marginMilliseconds = 300;
	public static float accelerationPrecision = (float) 0.9;
	public static float pitchPrecision = 0f; // pitch defines design of the
												// acceleration (if it is
												// braking or accelerating) (=0
												// by the moment)

	/**** Bluetooth related fields ****/
	// MY_UUID is the app's UUID string, also used by the server code?
	public static final UUID MY_UUID = UUID
			.fromString("04c6032b-0000-4000-8000-00805f9b34fc");
	// public static final UUID MY_UUID = UUID
	// .fromString("00001105-0000-1000-8000-00805F9B34FB");

	/**** Handler Section ****/
	// Constants that indicate the current connection state
	public static final int STATE_DISCONNECTED = 0; // we're doing nothing
	public static final int STATE_CONNECTING = 1;// initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 2; // connected to a remote device

	public static final int ACCEL_VALUE_MSG = 3;
	public static final int MAGNETIC_BEARING_MSG = 4;
	public static final int MOVEMENT_BEARING_MSG = 5;
	public static final int DECLINATION_MSG = 6;

	// ****calculate angles average
	public static int delta = 30; // acceptable error in each angle.
	public static int nIter = 10; // number of iteration to actualize the
									// average angle (1 iteration = 0.1 seconds,
									// aprox).
	// *****end*angles average

}
