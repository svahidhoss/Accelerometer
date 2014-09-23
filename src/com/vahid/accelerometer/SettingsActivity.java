package com.vahid.accelerometer;

import com.vahid.accelerometer.R;
import com.vahid.accelerometer.util.VahidConstants;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	private EditText precisionET, delayET;
	private Button b;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initViews();
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				actualizarSettings();
				finish();

			}
		});

		precisionET.setText("" + VahidConstants.ACCEL_THRESHOLD);
		delayET.setText("" + VahidConstants.WINDOW_SIZE_IN_MILI_S);

	}

	private void initViews() {
		setContentView(R.layout.activity_settings);
		precisionET = (EditText) findViewById(R.id.editTextPrecisionID);
		delayET = (EditText) findViewById(R.id.editTextDelayID);
		b = (Button) findViewById(R.id.buttonRefreshSettigns);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_m, menu);
		return true;

	}

	public void actualizarSettings() {
		try {
			VahidConstants.ACCEL_THRESHOLD = Float.parseFloat(precisionET
					.getText().toString());
		} catch (Exception e) {
			// TODO: handle exception
			VahidConstants.ACCEL_THRESHOLD = 0;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			VahidConstants.WINDOW_SIZE_IN_MILI_S = Long.parseLong(delayET
					.getText().toString());

		} catch (Exception e) {
			// TODO: handle exception
			VahidConstants.WINDOW_SIZE_IN_MILI_S = 0;
			// Toast.makeText(getApplicationContext(),
			// "Error saving, try again", Toast.LENGTH_SHORT).show();
		}

		Toast.makeText(
				getApplicationContext(),
				"Saved!\n  *precision = "
						+ VahidConstants.ACCEL_THRESHOLD
						+ "\n  *delay = " + VahidConstants.WINDOW_SIZE_IN_MILI_S,
				Toast.LENGTH_SHORT).show();

	}

}
