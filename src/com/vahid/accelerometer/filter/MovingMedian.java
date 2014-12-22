package com.vahid.accelerometer.filter;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class MovingMedian {
	private final int size;
	private int index;

	private PriorityQueue<Float> upperQueue;
	private LinkedList<Float> upperList;
	private PriorityQueue<Float> lowerQueue;
	private LinkedList<Float> lowerList;

	public MovingMedian(int size) {
		this.index = 0;
		this.size = size;

		upperList = new LinkedList<>();
		lowerList = new LinkedList<>();

		lowerQueue = new PriorityQueue<Float>(20, new Comparator<Float>() {

			@Override
			public int compare(Float o1, Float o2) {
				return -o1.compareTo(o2);
			}

		});
		upperQueue = new PriorityQueue<Float>();
		upperQueue.add(Float.MAX_VALUE);
		lowerQueue.add(Float.MIN_VALUE);
	}

	public void pushValue(float newValue) {
		// adding the number to proper heap
		if (newValue >= upperQueue.peek()) {
			if (index >= size)
				upperQueue.remove(upperList.removeFirst());

			upperList.add(newValue);
			upperQueue.add(newValue);
		} else {
			if (index >= size)
				lowerQueue.remove(lowerList.removeFirst());

			lowerList.add(newValue);
			lowerQueue.add(newValue);
		}

		// balancing the heaps; doesn't happen for lists!
		if (upperQueue.size() - lowerQueue.size() == 2) {
			float temp = upperQueue.poll();
			lowerQueue.add(temp);
			
			upperList.remove(temp);
			lowerList.add(temp);
		}
		else if (lowerQueue.size() - upperQueue.size() == 2){
			float temp = upperQueue.poll();
			upperQueue.add(temp);
			
			lowerList.remove(temp);
			upperList.add(temp);
		}

		index++;
	}

	public double getMedian() {
		// returning the median
		if (upperQueue.size() == lowerQueue.size())
			return (upperQueue.peek() + lowerQueue.peek()) / 2.0;
		else if (upperQueue.size() > lowerQueue.size())
			return upperQueue.peek();
		else
			return lowerQueue.peek();

	}

}