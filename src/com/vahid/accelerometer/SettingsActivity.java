package com.vahid.accelerometer;

import com.vahid.accelerometer.R;
import com.vahid.accelerometer.util.Constants;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	private EditText etPrecision, etDelay, etWindowSize;
	private Button mButtonSave;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initViews();
		mButtonSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				actualizarSettings();
				finish();

			}
		});

		etPrecision.setText("" + Constants.ACCEL_THRESHOLD);
		etDelay.setText("" + Constants.WINDOW_SIZE_IN_MILI_S);
		etWindowSize.setText("" + Constants.WINDOW_SIZE);

	}

	private void initViews() {
		setContentView(R.layout.activity_settings);
		etPrecision = (EditText) findViewById(R.id.editTextPrecisionID);
		etDelay = (EditText) findViewById(R.id.editTextDelayID);
		etWindowSize = (EditText) findViewById(R.id.editTextWindowSizeID);
		mButtonSave = (Button) findViewById(R.id.buttonRefreshSettigns);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_m, menu);
		return true;

	}

	public void actualizarSettings() {
		try {
			Constants.ACCEL_THRESHOLD = Float.parseFloat(etPrecision
					.getText().toString());
		} catch (Exception e) {
			// TODO: handle exception
			Constants.ACCEL_THRESHOLD = 1;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			Constants.WINDOW_SIZE_IN_MILI_S = Long.parseLong(etDelay
					.getText().toString());

		} catch (Exception e) {
			// TODO: handle exception
			Constants.WINDOW_SIZE_IN_MILI_S = 1000;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			Constants.WINDOW_SIZE = Integer.parseInt(etWindowSize
					.getText().toString());

		} catch (Exception e) {
			// TODO: handle exception
			Constants.WINDOW_SIZE = 20;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}

		Toast.makeText(
				getApplicationContext(),
				"Saved!\n  *brake precision = "
						+ Constants.ACCEL_THRESHOLD
						+ "\n  *delay = " + Constants.WINDOW_SIZE_IN_MILI_S
						+ "\n  *window size= " + Constants.WINDOW_SIZE,
				Toast.LENGTH_SHORT).show();

	}

}
