package com.vahid.accelerometer;

import com.vahid.accelerometer.R;
import com.vahid.accelerometer.util.Constants;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	private EditText etPrecision, etDelay, etWindowSize;
	private Button mButtonSave;
	private CheckBox mGpsCheckBox, mBtCheckBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initViews();
		
		// set defalut values of the GPS and BT checkboxes.
		if (Constants.GPS_MODULE_EXISTS) {
			mGpsCheckBox.setChecked(true);
		} else {
			mGpsCheckBox.setChecked(false);
		}
		if (Constants.BT_MODULE_EXISTS) {
			mBtCheckBox.setChecked(true);
		} else {
			mBtCheckBox.setChecked(false);
		}
		
		// set up save button
		mButtonSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				actualizarSettings();
				finish();

			}
		});
		
		// set edit text views
		etPrecision.setText("" + Constants.ACCEL_THRESHOLD);
		etDelay.setText("" + Constants.WINDOW_SIZE_IN_MILI_SEC);
		etWindowSize.setText("" + Constants.WINDOW_SIZE);

	}

	//
	private void initViews() {
		setContentView(R.layout.activity_settings);
		etPrecision = (EditText) findViewById(R.id.editTextPrecisionID);
		etDelay = (EditText) findViewById(R.id.editTextDelayID);
		etWindowSize = (EditText) findViewById(R.id.editTextWindowSizeID);
		mButtonSave = (Button) findViewById(R.id.buttonRefreshSettigns);
		mGpsCheckBox = (CheckBox) findViewById(R.id.gpsCheckBox);
		mBtCheckBox = (CheckBox) findViewById(R.id.btCheckBox);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_m, menu);
		return true;

	}

	/**
	 * saving the changes in settings information.
	 */
	public void actualizarSettings() {
		try {
			Constants.ACCEL_THRESHOLD = Float.parseFloat(etPrecision.getText()
					.toString());
		} catch (Exception e) {
			// TODO: handle exception
			Constants.ACCEL_THRESHOLD = 1;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			Constants.WINDOW_SIZE_IN_MILI_SEC = Long.parseLong(etDelay.getText()
					.toString());

		} catch (Exception e) {
			// TODO: handle exception
			Constants.WINDOW_SIZE_IN_MILI_SEC = 1000;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			Constants.WINDOW_SIZE = Integer.parseInt(etWindowSize.getText()
					.toString());

		} catch (Exception e) {
			// TODO: handle exception
			Constants.WINDOW_SIZE = 20;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}

		// Displaying the information changes.
		Toast.makeText(
				getApplicationContext(),
				"Saved!\n  *brake precision = " + Constants.ACCEL_THRESHOLD
						+ "\n  *delay = " + Constants.WINDOW_SIZE_IN_MILI_SEC
						+ "\n  *window size= " + Constants.WINDOW_SIZE,
				Toast.LENGTH_SHORT).show();

	}

	/**
	 * Manages all the check boxes of this Activity.
	 * 
	 * @param view
	 */
	public void onCheckboxClicked(View view) {
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		// Check which checkbox was clicked
		switch (view.getId()) {
		// enable/disable GPS
		case R.id.gpsCheckBox:
			if (checked)
				Constants.GPS_MODULE_EXISTS = true;
			else
				Constants.GPS_MODULE_EXISTS = false;
			break;
		// enable/disable BT to write send state back to BT server (PC)
		case R.id.btCheckBox:
			if (checked)
				Constants.BT_MODULE_EXISTS = true;
			else
				Constants.BT_MODULE_EXISTS = false;
			break;
		}
	}

}
