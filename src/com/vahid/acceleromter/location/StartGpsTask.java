package com.vahid.acceleromter.location;

import java.util.concurrent.Callable;

import android.content.Context;
import android.location.LocationManager;

import com.vahid.accelerometer.util.Constants;

public class StartGpsTask<V> implements Callable<V> {
	private MyLocationListener myLocationListener;
	private LocationManager mLocationManager;
	
	public StartGpsTask(MyLocationListener myLocationListener) {
		this.myLocationListener = myLocationListener;
	}
	
	
	@Override
	public V call() throws Exception {
		// TODO Auto-generated method stub
//		myLocationListener = new MyLocationListener(
//				getApplicationContext(), mHandler);
//		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				Constants.GPS_MIN_TIME_MIL_SEC,
				Constants.GPS_MIN_DISTANCE_METER, myLocationListener);
		return null;
	}

}
