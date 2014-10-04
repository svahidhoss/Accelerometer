package com.vahid.accelerometer.filter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vahid.accelerometer.util.Constants;

import android.os.Handler;

/**
 * The class used for having a Moving Average low pass filter based on time (not
 * on the size of the window). Its application here is to detect the brakes. To
 * use this class you start pushing values to it whenever you detect a
 * deceleration incident, it keeps getting brake the acceleration values and if
 * the values continue for more than mWindowTimeFrame (a second_ a brake
 * incident is detected.
 * 
 * @author Vahid
 *
 */
public class MovingAverageTime {
	private List<Float> mCircularBuffer;
	private float mAverage;
	private Date mStartDate;
	private long mWindowTimeFrame;
	private boolean oneMinuteOfValuesWithin = false;
	private Handler mHandler;
	private boolean brakeDetected = true;

	public MovingAverageTime(long windowTimeFrame, Handler handler) {
		mCircularBuffer = new ArrayList<Float>();
		mAverage = 0;
		mWindowTimeFrame = windowTimeFrame;
		mHandler = handler;
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
	public void pushValue(float newValue, Date newValueDate) {
		if (mCircularBuffer.isEmpty()) {
			createPrimeBuffer(newValue, newValueDate);
		} else {
			mCircularBuffer.add(newValue);
			mAverage = mAverage + (newValue - mAverage)
					/ mCircularBuffer.size();
		}
		if (isMoreThanWindowFrame(newValueDate) && brakeDetected) {
			// Send Message about the average of a brake.
//			mHandler.obtainMessage(Constants.BRAKE_DETECTED_MSG, mAverage);
			mHandler.obtainMessage(Constants.BRAKE_DETECTED_MSG);
			brakeDetected = false;
		}
	}

	/**
	 * Determines weather the new time value coming is more within the time
	 * frame of the moving average we're calculating.
	 * 
	 * @param newValueDate
	 *            the value to do the comparison with the current start date of
	 *            averaging.
	 * @return true if it is within and false if not.
	 */
	private boolean isMoreThanWindowFrame(Date newValueDate) {
		if (newValueDate.getTime() - mStartDate.getTime() >= mWindowTimeFrame) {
			return true;
		}
		return false;
	}

	/**
	 * Create the initial buffer for averaging.
	 * 
	 * @param initialValue
	 */
	private void createPrimeBuffer(float initialValue, Date newValueDate) {
		mCircularBuffer.add(initialValue);
		mAverage = initialValue;
		mStartDate = newValueDate;
	}

	/**
	 * Gets the size of the list.
	 * 
	 * @return the count of the list.
	 */
	public int getCount() {
		return mCircularBuffer.size();
	}

	/**
	 * Method to be called when the brake finishes.
	 */
	public void clearValues() {
		if (!mCircularBuffer.isEmpty()) {
			// Empty the buffer for getting new values to average.
			mCircularBuffer.clear();
		}
		brakeDetected = true;
	}

}
