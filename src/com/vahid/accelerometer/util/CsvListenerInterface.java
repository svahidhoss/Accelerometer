package com.vahid.accelerometer.util;


public interface CsvListenerInterface {

	public void enableSaveToFile();

	public void disableSaveToFile();
	
	public void setCsvFile(CsvFileWriter csvFile);
}
