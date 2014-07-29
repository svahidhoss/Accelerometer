package com.alexcar.accelerometer;

import java.nio.ByteBuffer;

public class mMath {

	public double module(double a, double b, double c) {
		double mod = (Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)
				+ Math.pow(c, 2)));
		return mod;
	}

	public String redondeo(double num, int decimales) {
		String res;
		double cifras = Math.pow(10.0, decimales);
		res = "" + (Math.round(num * cifras) / cifras);
		return res;
	}

	public String redondeo(float num, int decimales) {
		String res;
		double cifras = Math.pow(10.0, decimales);
		res = "" + (Math.round(num * cifras) / cifras);
		return res;
	}

	public float toRadians(float alpha) {
		float rad = (float) ((alpha * Math.PI) / 180);
		return rad;
	}

	public float[] cancelGravity(float[] acceleromterValues,
			float[] orientationValues) {

		int signumG = 1;

		if (Math.abs(orientationValues[1]) > 90) {
			signumG = -1;
		} else {
			signumG = 1;
		}

		float[] f = new float[3];

		f[0] = (float) (acceleromterValues[0] - Constants.gravity
				* Math.sin(toRadians(orientationValues[2])));
		f[1] = (float) (acceleromterValues[1] + Constants.gravity
				* Math.sin(toRadians(orientationValues[1])));
		f[2] = (float) (acceleromterValues[2] - signumG
				* (Math.sqrt(-Math.pow(
						Constants.gravity
								* Math.sin(toRadians(orientationValues[2])), 2)
						- Math.pow(
								Constants.gravity
										* Math.sin(toRadians(orientationValues[1])),
								2) + Math.pow(Constants.gravity, 2))));

		return f;
	}

	public float[] convertReference2(float[] values, float[] orientationValues) {

		float x = values[0];
		float y = values[1];
		float z = values[2];
		// float azimuth_angle = toRadians(orientationValues[0]); //not used
		float pitch_angle = toRadians(orientationValues[1]);
		float roll_angle = toRadians(orientationValues[2]);

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

	public byte[] toByteArray(double value) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
	}

	public double toDouble(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getDouble();
	}

	public byte[] concatenateBytes(byte[] b1, byte[] b2) {
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

}
