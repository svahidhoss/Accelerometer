package com.vahid.acceleromter.location;

import android.location.LocationManager;

import com.vahid.accelerometer.util.Constants;

public class StartGpsTask implements Runnable {
	private MyLocationListener myLocationListener;
	private LocationManager mLocationManager;

	public StartGpsTask(MyLocationListener myLocationListener) {
		this.myLocationListener = myLocationListener;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
//		myLocationListener = new MyLocationListener(getApplicationContext(),
//				mHandler);
//		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				Constants.GPS_MIN_TIME_MIL_SEC,
				Constants.GPS_MIN_DISTANCE_METER, myLocationListener);
	}

}
