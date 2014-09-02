package com.vahid.acceleromter.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

public class MyLocationListener implements LocationListener {

	private Context parentContext;

	public MyLocationListener(Context context) {
		parentContext = context;
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
