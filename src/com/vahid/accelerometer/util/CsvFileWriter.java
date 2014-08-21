package com.vahid.accelerometer.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class CsvFileWriter {
	private static final String DIRECTORY_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/Sensors Capture";
	private boolean captureState = true;
	private String captureStateText = null;
	private PrintWriter captureFile = null;

	/**
	 * Default constructor that creates a new CSV file based on the current time
	 * in the /Sensors capture folder.
	 */
	public CsvFileWriter() {
		checkDirectoryToCreate(DIRECTORY_PATH);
		File captureFileName = new File(DIRECTORY_PATH, getFileNameByDate());
		captureStateText = "Capture: " + captureFileName.getAbsolutePath();
		try {
			captureFile = new PrintWriter(
					new FileWriter(captureFileName, false));
		} catch (IOException ex) {
			if (Constants.DEBUG)
				Log.e(Constants.LOG_TAG, ex.getMessage(), ex);
			captureStateText = "Capture: " + ex.getMessage();
		}
	}

	/**
	 * Method that writes the values of the sensors passed with constructor to
	 * the file that has been created.
	 * 
	 * @param values
	 */
	public boolean writeToFile(float values[]) {

		/*
		 * StringBuilder b = new StringBuilder(); for (int i = 0; i <
		 * values.length; ++i) { if (i > 0) b.append(" , ");
		 * b.append(Float.toString(values[i])); } if (Constants.DEBUG)
		 * Log.d(Constants.LOG_TAG, "onSensorChanged: [" + b + "]");
		 */

		if (captureFile != null) {
			for (int i = 0; i < values.length; ++i) {
				if (i > 0)
					captureFile.print(",");
				captureFile.print(Float.toString(values[i]));
			}
			return true;
		}
		return false;
	}

	/**
	 * Method that writes the value of a sensor passed with constructor to the
	 * file that has been created.
	 * 
	 * @param value float value to be written to the end of file.
	 * @param endOfLine write an eol to the end of file if true.
	 * @return
	 */
	public boolean writeToFile(float value, boolean endOfLine) {
		if (captureFile != null) {
			captureFile.print(",");
			captureFile.print(Float.toString(value));
			if (endOfLine) {
				captureFile.println();
			}
			else {
				captureFile.print(",");
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns a unique file name for the csv file that needs to be created
	 * based on the current time.
	 * 
	 * @return
	 */
	private String getFileNameByDate() {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault());
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

	public boolean isCaptureState() {
		return captureState;
	}

	public void setCaptureState(boolean captureState) {
		this.captureState = captureState;
	}

	public String getCaptureStateText() {
		return captureStateText;
	}

	public void setCaptureStateText(String captureStateText) {
		this.captureStateText = captureStateText;
	}

	/**
	 * Closes the CaptureFile print writer. Flushes the writer and then closes
	 * the target. If an I/O error occurs, this writer's error flag is set to
	 * true.
	 */
	public void closeCaptureFile() {
		if (captureFile != null) {
			captureFile.close();
		}
	}

}
