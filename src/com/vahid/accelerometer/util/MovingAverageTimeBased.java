package com.vahid.accelerometer.util;

import java.util.ArrayList;
import java.util.List;

public class MovingAverageTimeBased implements Runnable {
	private List<Float> mCircularBuffer;
	private float mAverage;

	public MovingAverageTimeBased() {
		mCircularBuffer = new ArrayList<Float>();
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
		if (mCircularBuffer.isEmpty()) {
			createPrimeBuffer(newValue);
		} else {
			mCircularBuffer.add(newValue);
			mAverage = mAverage + (newValue - mAverage)
					/ mCircularBuffer.size();
		}

	}

	/**
	 * Create the initial buffer for averaging.
	 * 
	 * @param initialValue
	 */
	private void createPrimeBuffer(float initialValue) {
		mCircularBuffer.add(initialValue);
		mAverage = initialValue;
	}

	public long getCount() {
		return mCircularBuffer.size();
	}

	/**
	 * Function that detects if a brake has occurred. It should be called by a
	 * scheduler after a certain interval.
	 * 
	 * @return whether there was a break, an acceleration or no move.
	 */
	public int detectSituation() {
		if (mAverage <= Constants.BRAKE_THRESHOLD) {
			return Constants.BRAKE_DETECTED;
		}
		if (mAverage >= Constants.ACCEL_THRESHOLD) {
			return Constants.ACCEL_DETECTED;
		}
		return Constants.NO_MOVE_DETECTED;
	}

	@Override
	public void run() {
		detectSituation();
		// Empty the buffer for getting new values to average.
		mCircularBuffer.clear();
	}
}
