package com.alexcar.accelerometer;

import java.util.UUID;

import android.hardware.SensorManager;

public class Constants {
	public static float precision = (float) 0.9;
	public static long marginMilliseconds = 300;
	public static double gravity = SensorManager.STANDARD_GRAVITY;
	static boolean manualGravity = false;
	public static float precisionPitch = 0f; // pitch defines de sign of the
												// acceleration (if it is
												// braking or accelerating) (=0
												// by the moment)

	// ******bluetooth****
	// Unique UUID for this application
	public static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); 
	
	// ----handler
	public static int CONNECTING_HANDLER = 1;
	public static int CONNECTED_HANDLER = 2;
	public static int DISCONNECTED_HANDLER = 3;

	public static boolean STATE_CONNECTED = false;
	// ----handler
	// ******end bluetooth***

	// ****calculate angles average
	public static int delta = 30; // acceptable error in each angle.
	public static int nIter = 10; // number of iteration to actualize the
									// average angle (1 iteration = 0.1 seconds,
									// aprox).
	// *****end*angles average

}