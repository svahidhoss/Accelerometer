package com.vahid.accelerometer.util;

public class MovingAverage {
	private float circularBuffer[];
	private float avg;
	private int circularIndex;
	private int count;

	public MovingAverage(int windowSize) {
		circularBuffer = new float[windowSize];
		count = 0;
		circularIndex = 0;
		avg = 0;
	}

	/**
	 * Get the current moving average.
	 */
	public float getMovingAverage() {
		return avg;
	}

	/**
	 * Push a new value to the buffer.
	 * 
	 * @param x
	 *            new value
	 */
	public void pushValue(float x) {
		if (count++ == 0) {
			primeBuffer(x);
		}
		float lastValue = circularBuffer[circularIndex];
		avg = avg + (x - lastValue) / circularBuffer.length;
		circularBuffer[circularIndex] = x;
		circularIndex = nextIndex(circularIndex);
	}

	public long getCount() {
		return count;
	}

	private void primeBuffer(float val) {
		for (int i = 0; i < circularBuffer.length; ++i) {
			circularBuffer[i] = val;
		}

		avg = val;
	}

	private int nextIndex(int curIndex) {
		if (curIndex + 1 >= circularBuffer.length) {
			return 0;
		}
		return curIndex + 1;
	}

	/**
	 * Function that detects if a brake has occurred.
	 * 
	 * @return true if occurred.
	 */
	public int detectSituation() {
		if (count >= circularBuffer.length) {
			if (avg <= Constants.BRAKE_THRESHOLD) {
				return Constants.BRAKE_DETECTED;
			}
			if (avg >= Constants.ACCEL_THRESHOLD) {
				return Constants.ACCEL_DETECTED;
			}
		}
		return Constants.NO_MOVE_DETECTED;

	}
}
