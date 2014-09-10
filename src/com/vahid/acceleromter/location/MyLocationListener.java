package com.vahid.acceleromter.location;

import java.util.ArrayList;

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
	// currently two minutes.
	private static final int MAX_ACCEPTED_TIME = 1000 * 60 * 2;
	// Main activity related fields
	private Context parentContext;
	private Handler mHandler;

	// Used for determining best locations
	private Location currentBestLocation;

	// calculating the bearing to the north?
	private ArrayList<Location> locations = new ArrayList<Location>();
	private MovingAverage latMovingAverage = new MovingAverage(5);
	private MovingAverage longMovingAverage = new MovingAverage(5);
	private float bearing;
	private float magneticDeclination;
	private float bearingFromMagneticNorth;

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

		// if it's the first time, or if the new location is better use it as
		// the new best location.
		if (isBetterLocation(location, currentBestLocation)
				|| currentBestLocation == null) {
			currentBestLocation = location;
		}

		bearing = location.getBearing();
		magneticDeclination = getMagneticDeclination(location);
		bearingFromMagneticNorth = bearing - magneticDeclination;

		String Text = "My current location is: \n" + "Latitude = "
				+ location.getLatitude() + "\n" + "Longitude = "
				+ location.getLongitude() + "\nMy Speed is: "
				+ location.getSpeed() + "\nMy Bearing is: " + bearing
				+ "\nMy Dclination is: " + magneticDeclination;

		Toast.makeText(parentContext, Text, Toast.LENGTH_SHORT).show();

		// sending back the degree between
		mHandler.obtainMessage(Constants.MOVEMENT_BEARING_MSG,
				bearingFromMagneticNorth).sendToTarget();

		if (savingToFile && csvLocationFile != null) {
			csvLocationFile.writeToFile(bearing, false);
			csvLocationFile.writeToFile(location.getSpeed(), false);
			csvLocationFile.writeToFile(bearingFromMagneticNorth, false);
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

	/**
	 * This methods returns the magnetic declination degree of horizontal
	 * component of the magnetic field from true north.
	 * 
	 * @param location
	 * @return declination degree
	 */
	public static float getMagneticDeclination(Location location) {
		GeomagneticField geoField = new GeomagneticField(
				(float) location.getLatitude(),
				(float) location.getLongitude(),
				(float) location.getAltitude(), System.currentTimeMillis());
		// West declination is reported in negative values
		// return -1*geoField.getDeclination();
		return geoField.getDeclination();
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix.
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > MAX_ACCEPTED_TIME;
		boolean isSignificantlyOlder = timeDelta < -MAX_ACCEPTED_TIME;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether two providers are the same
	 * 
	 * @param provider1
	 * @param provider2
	 * @return true if the same
	 */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
