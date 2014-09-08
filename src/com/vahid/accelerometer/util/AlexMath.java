package com.vahid.accelerometer.util;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.hardware.SensorManager;

public class AlexMath {
	public static double DEFAULT_ICLINATION = -13.505042;

	/**
	 * Cancels the effects of Gravity form accelerometer values by using
	 * orientation sensor.
	 * 
	 * @param acceleromterValues
	 * @param orientationValues
	 * @return
	 */
	public static float[] cancelGravity(float[] acceleromterValues,
			float[] orientationValues) {
		int signumG;

		if (Math.abs(orientationValues[1]) > 90) {
			signumG = -1;
		} else {
			signumG = 1;
		}

		float[] f = new float[3];

		f[0] = (float) (acceleromterValues[0] - SensorManager.STANDARD_GRAVITY
				* Math.sin(degreesToRadians(orientationValues[2])));
		f[1] = (float) (acceleromterValues[1] + SensorManager.STANDARD_GRAVITY
				* Math.sin(degreesToRadians(orientationValues[1])));
		f[2] = (float) (acceleromterValues[2] - signumG
				* (Math.sqrt(-Math.pow(
						SensorManager.STANDARD_GRAVITY
								* Math.sin(degreesToRadians(orientationValues[2])),
						2)
						- Math.pow(
								SensorManager.STANDARD_GRAVITY
										* Math.sin(degreesToRadians(orientationValues[1])),
								2)
						+ Math.pow(SensorManager.STANDARD_GRAVITY, 2))));
		return f;
	}

	public static float[] convertReference(float[] values,
			float[] orientationValues) {

		float x = values[0];
		float y = values[1];
		float z = values[2];
		// float azimuth_angle = toRadians(orientationValues[0]); //not used
		float pitch_angle = degreesToRadians(orientationValues[1]);
		float roll_angle = degreesToRadians(orientationValues[2]);

		float b = (float) (Math.sin(roll_angle) * Math.tan(pitch_angle));
		float a = (float) Math.sqrt(Math.abs((Math.cos(roll_angle))
				* (Math.cos(roll_angle)) - b * b));

		values[0] = (float) (x * a - z
				* (b * Math.sin(pitch_angle) + Math.sin(roll_angle)
						* Math.cos(pitch_angle)));
		values[1] = (float) (x * b + y * Math.cos(pitch_angle) + z * a
				* Math.sin(pitch_angle));
		values[2] = (float) (x * Math.sin(roll_angle) - y
				* Math.sin(pitch_angle) + z * a * Math.cos(pitch_angle));

		return values;
	}
	
	/**
	 * Used for conversion of a 2x2 matrices based on the input degree.
	 * @param values
	 * @param degree
	 * @return
	 */
	public static float[] convertReference(float[] values, float degree) {
		float degreeRadian = degreesToRadians(degree);
		float[] covertedValues = new float[] { 0, 0 };
		covertedValues[0] = (float) (Math.cos(degreeRadian) * values[0] - Math.sin(degreeRadian) * values[1]);
		covertedValues[1] = (float) (Math.sin(degreeRadian) * values[0] + Math.cos(degreeRadian) * values[1]);
		return covertedValues;
	}

	public static byte[] toByteArray(double value) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
	}

	public static double byteToDouble(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getDouble();
	}

	public static byte[] concatenateBytes(byte[] b1, byte[] b2) {
		int le = b1.length + b2.length;
		byte[] concat = new byte[le];

		for (int i = 0; i < le; i++) {
			if (i < b1.length) {
				concat[i] = b1[i];
			} else {
				concat[i] = b2[i - b1.length];

			}
		}
		return concat;
	}

	/**
	 * Rounds the number given based on the decimals value.
	 * 
	 * @param number
	 * @param decimals
	 * @return
	 */
	public static String round(double number, int decimals) {
		String res;
		double cifras = Math.pow(10.0, decimals);
		res = "" + (Math.round(number * cifras) / cifras);
		return res;
	}

	/**
	 * Converts degrees to Radian.
	 * 
	 * @param alpha
	 *            in degrees
	 * @return alpha in Radian.
	 */
	public static float degreesToRadians(float alpha) {
		float rad = (float) ((alpha * Math.PI) / 180);
		return rad;
	}

	/**
	 * Calculates the magnitude of any vector by using Pythagorean principle.
	 * 
	 * @param vectorComponents
	 * @return The vector magnitude.
	 */
	public static double getVectorMagnitude(float[] vectorComponents) {
		double result = 0;
		for (double d : vectorComponents) {
			result += Math.pow(d, 2);
		}
		return Math.sqrt(result);
	}

	// public static String getDate() {
	// SimpleDateFormat formatter = new SimpleDateFormat(
	// "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
	// Date now = new Date();
	// return formatter.format(now);
	// }

}
