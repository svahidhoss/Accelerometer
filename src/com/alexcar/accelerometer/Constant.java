package com.alexcar.accelerometer;

import android.hardware.SensorManager;

public class Constant {
	public static float  precision=(float) 0.9;
	public static long marginMilliseconds=300;
	public static double gravity=SensorManager.STANDARD_GRAVITY;
	static boolean  manualGravity=false; 
	public static float precisionPitch = 0f;  //pitch defines de sign of the acceleration (if it is braking or accelerating) (=0 by the moment)
	
	//******bluetooth****
	
	//----handler
	public static int CONNECTING_HANDLER = 1;
	public static int CONNECTED_HANDLER = 2;
	public static int DISCONNECTED_HANDLER = 3;
	
	public static boolean STATE_CONNECTED=false;
	//----handler
	//******end bluetooth***	
	
	
	
	//****calculate angles average
	public static int delta = 30; //acceptable error in each angle.
	public static int nIter = 10; // number of iteration to actualize the average angle (1 iteration = 0.1 seconds, aprox).
	//*****end*angles average

}
