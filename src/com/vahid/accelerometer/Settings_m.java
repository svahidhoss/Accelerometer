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

public class Settings_m extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_m);
		
		
		Button b = (Button) findViewById(R.id.buttonRefreshSettigns);
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				actualizarSettings();
				//startActivity(new Intent(getApplicationContext(), MainActivity.class));
				finish();
				
				
			}
		});
		

		EditText precisionET = (EditText) findViewById(R.id.editTextPrecisionID);
		precisionET.setText(""+Constants.precision);
		
		
		EditText delayET = (EditText) findViewById(R.id.editTextDelayID);
		delayET.setText(""+Constants.marginMilliseconds);
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_m, menu);
		return true;

	}
	
	
	public void actualizarSettings(){
		
		EditText precisionET = (EditText) findViewById(R.id.editTextPrecisionID);
		EditText delayET = (EditText) findViewById(R.id.editTextDelayID);
		
		try {
			Constants.precision = Float.parseFloat(precisionET.getText().toString());	
		} catch (Exception e) {
			// TODO: handle exception
			Constants.precision = 0;
			//Toast.makeText(getApplicationContext(), "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		try {
			Constants.marginMilliseconds = Long.parseLong(delayET.getText().toString());

		} catch (Exception e) {
			// TODO: handle exception
			Constants.marginMilliseconds = 0;
			//Toast.makeText(getApplicationContext(), "Error saving, try again", Toast.LENGTH_SHORT).show();
		}
		
		Toast.makeText(getApplicationContext(), "Saved!\n  *precision = "+Constants.precision+"\n  *delay = "+Constants.marginMilliseconds, Toast.LENGTH_SHORT).show();
		
	}
	


}

