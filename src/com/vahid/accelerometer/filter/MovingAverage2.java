package com.vahid.accelerometer.filter;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage2 {
	private final Queue<Double> window = new LinkedList<Double>();
	private final int size;
	private double sum;

	/**
	 * Constructor that accepts the size of the Queue.
	 * 
	 * @param period
	 */
	public MovingAverage2(int size) {
		assert size > 0 : "Period must be a positive integer";
		this.size = size;
	}

	/**
	 * Method to push a new value.
	 * 
	 * @param num
	 */
	public void pushValue(double num) {
		sum += num;
		window.add(num);
		if (window.size() > size) {
			sum -= window.remove();
		}
	}

	/**
	 * Function that returns the moving average.
	 * 
	 * @return
	 */
	public double getAverage() {
		// technically the average is undefined
		if (window.isEmpty())
			return 0;
		return sum / window.size();
	}

}
