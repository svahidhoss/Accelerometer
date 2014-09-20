package com.vahid.accelerometer.util;

import java.util.ArrayList;
import java.util.List;

public class MovingAverageTimeBased {
	private List<Float> mCircularBuffer;
	private float mAverage;
//	private int mCircularIndex;
//	private int mCount;
	private int mWindowSizeLimit;
	private float mInitialValue;

	public MovingAverageTimeBased(int windowSizeLimit) {
		mCircularBuffer = new ArrayList<Float>();
		mWindowSizeLimit = windowSizeLimit;
//		mCount = 0;
//		mCircularIndex = 0;
		mAverage = 0;
	}

	/**
	 * Get the current moving average.
	 */
	public float getMovingAverage() {
		return mAverage;
	}

	/**
	 * Push a new value to the buffer.
	 * 
	 * @param newValue
	 *            new value
	 */
	public void pushValue(float newValue) {
		if (mCircularBuffer.size() == 0) {
			createPrimeBuffer(newValue);
		}
		float lastValue = mCircularBuffer.get(mCircularBuffer.size() - 1);
		mAverage = mAverage + (newValue - lastValue) / mCircularBuffer.size();
		mCircularBuffer.add(newValue);

	}

	/**
	 * Create the initial buffer for averaging.
	 * @param initialValue
	 */
	private void createPrimeBuffer(float initialValue) {
		mCircularBuffer.add(initialValue);
		mAverage = initialValue;
		mInitialValue = initialValue;
	}
	
	
	public long getCount() {
		return mCircularBuffer.size();
	}

	/**
	 * Function that detects if a brake has occurred.
	 * 
	 * @return true if occurred.
	 */
	public int detectSituation() {
		if (mCircularBuffer.size() >= mWindowSizeLimit) {
			if (mAverage <= Constants.BRAKE_THRESHOLD) {
				return Constants.BRAKE_DETECTED;
			}
			if (mAverage >= Constants.ACCEL_THRESHOLD) {
				return Constants.ACCEL_DETECTED;
			}
		}
		return Constants.NO_MOVE_DETECTED;

	}
}
