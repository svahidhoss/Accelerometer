package com.vahid.accelerometer.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.vahid.accelerometer.util.Constants;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ConnectedThread extends Thread {
	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;
	Handler mHandler;

	public ConnectedThread(BluetoothSocket socket, Handler handler) {
		mmSocket = socket;
		mHandler = handler;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) {
		}

		mmInStream = tmpIn;
		mmOutStream = tmpOut;

	}

	public void run() {
		// buffer store for the stream
		byte[] buffer = new byte[1024];
		// bytes returned from read()
		int bytes;

		// Keep listening to the InputStream until an exception occurs
		while (true) {
			try {
				// Read from the InputStream
				bytes = mmInStream.read(buffer);
				// Send the obtained bytes to the UI activity
				// mHandler.obtainMessage(MESSAGE_READ, bytes, -1,
				// buffer).sendToTarget();

			} catch (IOException e) {

				break;
			}
		}
	}

	/* Call this from the main activity to send data to the remote device */
	public void write(byte[] bytes) {
		try {
			mmOutStream.write(bytes);
		} catch (IOException e) {
			// when error have occurred trying to write, it's because we are not
			// connected any more.
			sendMessageToHandler(Constants.STATE_DISCONNECTED);
		}
	}

	public void write(int out) {
		try {
			mmOutStream.write(out);

			System.out.println("alex - ok write int");
			// Share the sent message back to the UI Activity
			// mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1,
			// buffer)
			// .sendToTarget();
		} catch (IOException e) {
			// Log.e(TAG, "Exception during write", e);
			System.out.println("alex - error write int");
		}
	}

	/* Call this from the main activity to shutdown the connection */
	public void cancel() {
		try {
			mmSocket.close();
			sendMessageToHandler(Constants.STATE_DISCONNECTED);
		} catch (IOException e) {
		}
	}

	/**
	 * Method that sends back the current status of connection back to the
	 * handler (on Main Activity)
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