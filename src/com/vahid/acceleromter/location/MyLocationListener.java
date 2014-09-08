package com.vahid.acceleromter.location;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.vahid.accelerometer.util.AlexMath;
import com.vahid.accelerometer.util.CsvListenerInterface;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.CsvFileWriter;
import com.vahid.accelerometer.util.MovingAverage;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class MyLocationListener implements LocationListener,
		CsvListenerInterface {

	private Context parentContext;
	private Handler mHandler;

	// calculating the bearing to the north?
	private ArrayList<Location> locations = new ArrayList<Location>();
	private MovingAverage latMovingAverage = new MovingAverage(5);
	private MovingAverage longMovingAverage = new MovingAverage(5);
	private float bearing;
	private float magneticDeclination;

	// save to file view fields
	private boolean savingToFile = false;
	private CsvFileWriter csvLocationFile;

	public MyLocationListener(Context context, Handler mhHandler) {
		parentContext = context;
		this.mHandler = mhHandler;
	}

	// called when the listener is notified with a location update from the GPS,
	// Apparently the most important one.
	@Override
	public void onLocationChanged(Location location) {
		locationRecieved(location);
	}

	private void locationRecieved(Location location) {
		bearing = location.getBearing();
		magneticDeclination = getMagneticDeclination(location);

		String Text = "My current location is: \n" + "Latitude = "
				+ location.getLatitude() + "\n" + "Longitude = "
				+ location.getLongitude() + "\nMy Speed is: "
				+ location.getSpeed() + "\nMy Bearing is: " + bearing
				+ "\nMy Dclination is: " + magneticDeclination;

		Toast.makeText(parentContext, Text, Toast.LENGTH_SHORT).show();

		mHandler.obtainMessage(Constants.ROTATION_DEGREE_MSG, bearing + magneticDeclination).sendToTarget();


		if (savingToFile && csvLocationFile != null) {
			csvLocationFile.writeToFile(bearing, false);
			csvLocationFile.writeToFile(location.getSpeed(), false);
			csvLocationFile.writeToFile(bearing + magneticDeclination, false);
			csvLocationFile.writeToFile(location.getTime(), true);
		}
		// latMovingAverage.pushValue(location.);
		// if (locations.size() >= 10) {

		// }
		/*
		 * try { sendGPSLocations(location); } catch (JSONException e) {
		 * e.printStackTrace(); }
		 */
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

	@Override
	public void enableSaveToFile() {
		this.savingToFile = true;
	}

	@Override
	public void disableSaveToFile() {
		this.savingToFile = false;
	}

	@Override
	public void setCsvFile(CsvFileWriter csvLocationFile) {
		this.csvLocationFile = csvLocationFile;
	}

	public static float getMagneticDeclination(Location location) {
		GeomagneticField geoField = new GeomagneticField(
				(float) location.getLatitude(),
				(float) location.getLongitude(),
				(float) location.getAltitude(), System.currentTimeMillis());
		// West declination is reported in negative values
		// return -1*geoField.getDeclination();
		return geoField.getDeclination();
	}

}
