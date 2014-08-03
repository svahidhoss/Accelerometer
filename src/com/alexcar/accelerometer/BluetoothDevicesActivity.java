package com.alexcar.accelerometer;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothDevicesActivity extends Activity implements
		OnItemClickListener {

	private static final int REQUEST_ENABLE_BT = 1;

	// TODO what's this?
	static final String EXTRA_ADDRESS = "resultActivityExtraAddress";

	public static String address = "";

	private BluetoothAdapter mBluetoothAdapter;
	private Set<BluetoothDevice> pairedDevices;
	private ArrayAdapter<String> mArrayAdapter = null;

	private BroadcastReceiver mReceiver;
	private Button btnScan;
	private TextView tvLabel;
	private ProgressBar progressBar;

	ListView listaViewDevices;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_bluetooth_devices);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		initViews();
		initBluetooth();

		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	/**
	 * Function used to initialize the views of the activity
	 */
	private void initViews() {
		progressBar = (ProgressBar) findViewById(R.id.progressBarScaning);
		progressBar.setVisibility(View.GONE);

		listaViewDevices = (ListView) findViewById(R.id.listViewDevices);
		listaViewDevices.setOnItemClickListener(this);

		mArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, 0);
		listaViewDevices.setAdapter(mArrayAdapter);
		tvLabel = (TextView) findViewById(R.id.textViewLabel);
		tvLabel.setText("Pareid Devices:");
		btnScan = (Button) findViewById(R.id.buttonScan);
		btnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnScan.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				tvLabel.setText("Scaning for more devices...");
				mArrayAdapter.clear();
				initBluetooth();
				mBluetoothAdapter.startDiscovery();
			}
		});
	}

	private void initBluetooth() {
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mArrayAdapter
						.add(device.getName() + "\n" + device.getAddress());
			}
		} else {
			mArrayAdapter.add(getString(R.string.noDevicePaired));
		}

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					String str = device.getName() + "\n" + device.getAddress();
					for (int i = 0; i < mArrayAdapter.getCount(); i++) {
						if (mArrayAdapter.getItem(i).equals(str)) {
							break;
						} else if (i == mArrayAdapter.getCount() - 1) {
							mArrayAdapter.add(str);
						}
					}

				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
						.equals(action)) {
					mBluetoothAdapter.cancelDiscovery();
					if (!mArrayAdapter.isEmpty()) {
						tvLabel.setText("Available devices:");
					} else {
						tvLabel.setText("There is no Devices detected!");
					}
					progressBar.setVisibility(View.GONE);
					btnScan.setVisibility(View.VISIBLE);
					// Toast.makeText(getApplicationContext(),
					// "fin Discovering", Toast.LENGTH_SHORT).show();

				} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_OFF) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned off", Toast.LENGTH_SHORT)
								.show();
						setResultCode(RESULT_CANCELED);
						finish();

					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
						Toast.makeText(getApplicationContext(),
								"Bluetooth turned ON", Toast.LENGTH_SHORT)
								.show();

					}
				}

			}
		};

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		unregisterReceiver(mReceiver);

		Intent data = new Intent();
		data.putExtra(EXTRA_ADDRESS, address);

		setResult(Activity.RESULT_CANCELED, data);

		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				// Toast.makeText(getApplicationContext(),
				// "Turning ON Bluetooth...", Toast.LENGTH_SHORT).show();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Bluetooth is required.",
						Toast.LENGTH_SHORT).show();
			}

		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (arg0.getId() == R.id.listViewDevices) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();

			}
			String auxString = mArrayAdapter.getItem(arg2).toString();

			address = auxString.substring(auxString.length() - 17);
			// Toast.makeText(this, "address: "+address,
			// Toast.LENGTH_SHORT).show();

			Intent intentData = new Intent();
			intentData.putExtra(EXTRA_ADDRESS, address);

			setResult(Activity.RESULT_OK, intentData);

			finish();
		}

	}

}
