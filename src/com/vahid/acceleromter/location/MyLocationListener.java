package com.vahid.acceleromter.location;

import java.util.ArrayList;

import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.MovingAverage;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class MyLocationListener implements LocationListener {

	private Context parentContext;
	private Handler mHandler;

	// calculating the bearing to the north?
	private ArrayList<Location> locations = new ArrayList<Location>();
	private MovingAverage latMovingAverage = new MovingAverage(5);
	private MovingAverage longMovingAverage = new MovingAverage(5);
	private float bearing;

	public MyLocationListener(Context context, Handler mhHandler) {
		parentContext = context;
		this.mHandler = mhHandler;
	}

	// called when the listener is notified with a location update from the GPS,
	// Apparently the most important one.
	@Override
	public void onLocationChanged(Location location) {
		String Text = "My current location is: \n" + "Latitude = "
				+ location.getLatitude() + "\n" + "Longitude = "
				+ location.getLongitude();

		Toast.makeText(parentContext, Text, Toast.LENGTH_SHORT)
				.show();
		bearing = location.getBearing();
		mHandler.obtainMessage(Constants.LOC_VALUE_MSG,
				bearing).sendToTarget();
//		latMovingAverage.pushValue(location.);
//		if (locations.size() >= 10) {
			
//		}
/*		try {
			sendGPSLocations(location);
		} catch (JSONException e) {
			e.printStackTrace();
		}*/
	}

	// called when the GPS provider is turned off (user turning off the GPS on
	// the phone)
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	// called when the GPS provider is turned on (user turning on the GPS on the
	// phone)
	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(parentContext, "Gps Enabled", Toast.LENGTH_SHORT).show();
	}

	// called when the status of the GPS provider changes
	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(parentContext, "Gps Disabled", Toast.LENGTH_SHORT)
				.show();
	}

}
