package com.vahid.accelerometer.util;

public class MovingAverage {
	private float mCircularBuffer[];
	private float mAverage;
	private int mCircularIndex;
	private int mCount;

	public MovingAverage(int windowSize) {
		mCircularBuffer = new float[windowSize];
		mCount = 0;
		mCircularIndex = 0;
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
		if (mCount++ == 0) {
			primeBuffer(newValue);
		}
		float lastValue = mCircularBuffer[mCircularIndex];
		mAverage = mAverage + (newValue - lastValue) / mCircularBuffer.length;
		mCircularBuffer[mCircularIndex] = newValue;
		mCircularIndex = findNextIndex(mCircularIndex);
	}

	public long getCount() {
		return mCount;
	}

	private void primeBuffer(float val) {
		for (int i = 0; i < mCircularBuffer.length; ++i) {
			mCircularBuffer[i] = val;
		}

		mAverage = val;
	}

	private int findNextIndex(int curIndex) {
		if (curIndex + 1 >= mCircularBuffer.length) {
			return 0;
		}
		return curIndex + 1;
	}

}
