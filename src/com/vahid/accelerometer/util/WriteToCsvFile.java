package com.vahid.accelerometer.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class WriteToCsvFile {
	private static final String DIRECTORY_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/Sensors capture";
	private boolean captureState = true;
	private String captureStateText = null;
	private PrintWriter captureFile = null;

	public WriteToCsvFile(float[] values) {
		createFile();
		writeToFile(values);
	}

	private void createFile() {
		if (captureState) {
			checkDirectoryToCreate(DIRECTORY_PATH);
			File captureFileName = new File(DIRECTORY_PATH, getFileNameByDate());
			captureStateText = "Capture: " + captureFileName.getAbsolutePath();
			try {
				captureFile = new PrintWriter(new FileWriter(captureFileName,
						false));
			} catch (IOException ex) {
				if (Constants.DEBUG) {
					Log.e(Constants.LOG_TAG, ex.getMessage(), ex);
				}
				captureStateText = "Capture: " + ex.getMessage();
			}
		} else {
			captureStateText = "Capture: OFF";
		}
		// TextView t = (TextView) findViewById(R.id.capturestate);
		// t.setText(captureStateText);
	}

	/**
	 * Method that writes the values of the sensors passed with constructor to
	 * the file that has been created.
	 * 
	 * @param values
	 */
	private void writeToFile(float values[]) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < values.length; ++i) {
			if (i > 0)
				b.append(" , ");
			b.append(Float.toString(values[i]));
		}
		if (Constants.DEBUG) {
			Log.d(Constants.LOG_TAG, "onSensorChanged: [" + b + "]");
		}
		if (captureFile != null) {
			for (int i = 0; i < values.length; ++i) {
				if (i > 0)
					captureFile.print(",");
				captureFile.print(Float.toString(values[i]));
			}
			captureFile.println();
		}
	}

	/**
	 * Returns a unique file name for the csv file that needs to be created
	 * based on the current time.
	 * 
	 * @return
	 */
	private String getFileNameByDate() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Date now = new Date();
		String fileName = "Capture_" + formatter.format(now) + ".csv";
		return fileName;
	}

	/**
	 * Checks if the path given is directing to a directory in the phone or not.
	 * If not it would create it.
	 * 
	 * @param directoryPath
	 * @return
	 */
	private void checkDirectoryToCreate(String directoryPath) {
		File direcory = new File(directoryPath);
		if (!direcory.isDirectory()) {
			direcory.mkdirs();
		}
	}
}
