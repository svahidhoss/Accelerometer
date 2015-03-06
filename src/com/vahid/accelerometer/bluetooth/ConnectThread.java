package com.vahid.accelerometer.bluetooth;

import java.io.IOException;

import com.vahid.accelerometer.util.Constants;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectThread extends Thread {
	private final BluetoothSocket bluetoothSocket;
	// private final BluetoothDevice mmDevice;
	private Handler mHandler;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	public ConnectThread(BluetoothDevice device, Handler handler) {
		// Use a temporary object that is later assigned to bluetoothSocket
		// because bluetoothSocket is final
		BluetoothSocket tmpSocket = null;
		// mmDevice = device;
		mHandler = handler;

		// Get a BluetoothSocket to connect with the given Bluetooth Device
		try {
			tmpSocket = device
					.createRfcommSocketToServiceRecord(Constants.MY_UUID);
		} catch (IOException e) {
			sendMessageToHandler(Constants.STATE_DISCONNECTED);
		}
		bluetoothSocket = tmpSocket;

	}

	public void run() {
		// Cancel discovery because it will slow down the connection
		mBluetoothAdapter.cancelDiscovery();

		// handler. we use it to know when device has been connected or
		// disconnected in the UI activity.
		sendMessageToHandler(Constants.STATE_CONNECTING);

		try {
			// Connect the device through the socket. This will block
			// until it succeeds or throws an exception
			bluetoothSocket.connect();

			// handler
			sendMessageToHandler(Constants.STATE_CONNECTED);

			// ********handler***
			if (Constants.DEBUG)
				Log.d(Constants.LOG_TAG, "Application is connected");

		} catch (IOException connectException) {
			// Unable to connect; close the socket and get out
			if (Constants.DEBUG)
				Log.d(Constants.LOG_TAG, "Application is not connected");
			sendMessageToHandler(Constants.STATE_DISCONNECTED);
			try {
				bluetoothSocket.close();
			} catch (IOException closeException) {
			}
			return;
		}

		// Do work to manage the connection (in a separate thread)
		// manageConnectedSocket(bluetoothSocket);
	}

	/** 
	 * Will cancel an in-progress connection, and close the socket.
	 * 
	 */
	public void cancel() {
		try {
			bluetoothSocket.close();
			sendMessageToHandler(Constants.STATE_DISCONNECTED);
		} catch (IOException e) {
		}
	}

	public BluetoothSocket getBluetoothSocket() {
		return bluetoothSocket;
	}

	/**
	 * Method that sends back the current status of connection back to the
	 * handler (on Main Activity).
	 * 
	 * @param messageNumber
	 */
	private void sendMessageToHandler(int messageNumber) {
		Message msgException = new Message();
		msgException.setTarget(mHandler);
		msgException.what = messageNumber;
		msgException.sendToTarget();
	}
}