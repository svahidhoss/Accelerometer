package com.alexcar.accelerometer;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class ConnectThread extends Thread {
	private final BluetoothSocket mmSocket;
	private final BluetoothDevice mmDevice;
	Handler mHandler;
	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	public ConnectThread(BluetoothDevice device, Handler handler,
			Context context) {
		// Use a temporary object that is later assigned to mmSocket,
		// because mmSocket is final
		BluetoothSocket tmpSocket = null;
		mmDevice = device;
		mHandler = handler;

		// Get a BluetoothSocket to connect with the given BluetoothDevice
		try {
//			DeviceUuidFactory newDeviceUuidFactory = new DeviceUuidFactory(
//					context);
//			tmpSocket = device.createRfcommSocketToServiceRecord(newDeviceUuidFactory.getDeviceUuid());
			tmpSocket = device.createRfcommSocketToServiceRecord(Constants.MY_UUID);
		} catch (IOException e) {
			Message msgException = new Message();
			msgException.setTarget(mHandler);
			msgException.what = Constants.DISCONNECTED_HANDLER;
			msgException.sendToTarget();
		}
		mmSocket = tmpSocket;

	}

	public void run() {
		// Cancel discovery because it will slow down the connection
		mBluetoothAdapter.cancelDiscovery();

		Message msg1 = new Message(); // handler. we use it to know when de
										// device has been connected or
										// disconnected in the UI activity
		msg1.setTarget(mHandler);

		msg1.what = Constants.CONNECTING_HANDLER;
		msg1.sendToTarget();

		try {
			// Connect the device through the socket. This will block
			// until it succeeds or throws an exception
			mmSocket.connect();

			// handler
			Message msg2 = new Message();
			msg2.setTarget(mHandler);
			msg2.what = Constants.CONNECTED_HANDLER;
			msg2.setTarget(mHandler);
			msg2.sendToTarget();

			// ********handler***
			System.out.println("alex - connected");

		} catch (IOException connectException) {
			// Unable to connect; close the socket and get out
			System.out.println("alex - NO connected");
			Message msgException = new Message();
			msgException.setTarget(mHandler);
			msgException.what = Constants.DISCONNECTED_HANDLER;
			msgException.sendToTarget();

			try {
				mmSocket.close();
			} catch (IOException closeException) {
			}
			return;
		}

		// Do work to manage the connection (in a separate thread)
		// manageConnectedSocket(mmSocket);
	}

	/** Will cancel an in-progress connection, and close the socket */
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {
		}
	}

	public BluetoothSocket getBTSocket() {
		return mmSocket;
	}
}