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
	private static final String ACCEL_SENSOR = "accelerometer";
	private static final String LOC_SENSOR = "location";
	private static final String DIRECTORY_PATH = Environment
			.getExternalStorageDirectory().getPath() + "/Sensors Capture";
	private boolean captureState = true;
	private String captureStateText = null;
	private PrintWriter captureFileWriter = null;
	private String captureFileName;

	/**
	 * Default constructor that creates a new CSV file based on the current time
	 * in the /Sensors capture folder.
	 */
	public CsvFileWriter(String fileName) {
		checkDirectoryToCreate(DIRECTORY_PATH);
		captureFileName = getFileNameByDate(fileName);
		File captureFile = new File(DIRECTORY_PATH, captureFileName);
		captureStateText = "Capture: " + captureFile.getAbsolutePath();
		try {
			captureFileWriter = new PrintWriter(new FileWriter(captureFile,
					false));
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
	public boolean writeToFile(float values[], boolean endOfLine) {
		if (captureFileWriter != null) {
			for (int i = 0; i < values.length; ++i) {
				if (i > 0)
					captureFileWriter.print(",");
				captureFileWriter.print(Float.toString(values[i]));
			}
			if (endOfLine) {
				captureFileWriter.println();
			} else {
				captureFileWriter.print(",");
			}
			return true;
		}
		return false;
	}

	/**
	 * Method that writes the value of a sensor passed with constructor to the
	 * file that has been created.
	 * 
	 * @param startOfLine
	 *            if the value to be written is at the beginning of the line.
	 * @param value
	 *            float value to be written to the end of file.
	 * @param endOfLine
	 *            write an eol to the end of file if true.
	 * @return
	 */
	public boolean writeToFile(float value, boolean endOfLine) {
		if (captureFileWriter != null) {
			captureFileWriter.print(Float.toString(value));
			if (endOfLine) {
				captureFileWriter.println();
			} else {
				captureFileWriter.print(",");
			}
			return true;
		}
		return false;
	}

	/**
	 * Method that writes the string passed to the file that has been created.
	 * 
	 * @param startOfLine
	 *            if the value to be written is at the beginning of the line.
	 * @param value
	 *            float value to be written to the end of file.
	 * @param endOfLine
	 *            write an eol to the end of file if true.
	 * @return
	 */
	public boolean writeToFile(String value, boolean endOfLine) {
		if (captureFileWriter != null) {
			captureFileWriter.print(value);
			if (endOfLine) {
				captureFileWriter.println();
			} else {
				captureFileWriter.print(",");
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
	private String getFileNameByDate(String fileName) {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault());
		Date now = new Date();
		String fianlFileName = fileName + "_capture_" + formatter.format(now) + ".csv";
		return fianlFileName;
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

	public String getCaptureFileName() {
		return captureFileName;
	}

	/**
	 * Closes the CaptureFile print writer. Flushes the writer and then closes
	 * the target. If an I/O error occurs, this writer's error flag is set to
	 * true.
	 */
	public void closeCaptureFile() {
		if (captureFileWriter != null) {
			captureFileWriter.close();
		}
	}

}
