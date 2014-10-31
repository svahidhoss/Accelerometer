package com.vahid.accelerometer.bluetooth;

import java.util.HashSet;
import java.util.Set;

import com.vahid.accelerometer.R;
import com.vahid.accelerometer.util.Constants;
import com.vahid.accelerometer.util.MathUtil;

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
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothDevicesActivity extends Activity implements
		OnItemClickListener {

	private static final int REQUEST_ENABLE_BT = 1;

	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	public static String EXTRA_DEVICE_NAME = "device_name";
	public static String EXTRA_DEVICE_GROUP_NAME = "device_group_name";

	private BluetoothAdapter mBluetoothAdapter;
	private Set<BluetoothDevice> pairedDevices;
	private ArrayAdapter<String> bluetoothPairedDevicesArrayAdapter,
			bluetoothDevicesArrayAdapter;
	private ListView listaViewPairedDevices, listaViewFoundDevices;

	private Button btnScan;
	private TextView tvPairedDevicesLabel, tvFoundDevicesLabel;
	
	// temporary list used to remove duplicates from foundDevicesAdapter
	private Set<BluetoothDevice> tmpFoundDevicesList = new HashSet<BluetoothDevice>();

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
		addPairedDevices();
		registerReceiver();
	}

	/**
	 * Function used to initialize the views of the activity
	 */
	private void initViews() {
		listaViewPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
		listaViewPairedDevices.setOnItemClickListener(this);
		listaViewFoundDevices = (ListView) findViewById(R.id.lvFoundDevices);
		listaViewFoundDevices.setOnItemClickListener(this);

		bluetoothDevicesArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, 0);
		bluetoothPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, 0);

		listaViewPairedDevices.setAdapter(bluetoothPairedDevicesArrayAdapter);
		listaViewFoundDevices.setAdapter(bluetoothDevicesArrayAdapter);

		tvPairedDevicesLabel = (TextView) findViewById(R.id.tvPairedDevicesTitle);
		tvFoundDevicesLabel = (TextView) findViewById(R.id.tvFoundDevicesTitle);

		btnScan = (Button) findViewById(R.id.btnScan);
		btnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnScan.setVisibility(View.GONE);
				// Indicate scanning in the title
				setProgressBarIndeterminateVisibility(true);
				setTitle("Scanning for devices...");
				tvFoundDevicesLabel.setVisibility(View.VISIBLE);

				bluetoothDevicesArrayAdapter.clear();
				if (mBluetoothAdapter.isDiscovering()) {
					mBluetoothAdapter.cancelDiscovery();
				}
				mBluetoothAdapter.startDiscovery();
			}
		});
	}

	private void addPairedDevices() {
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			tvPairedDevicesLabel.setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				bluetoothPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			bluetoothPairedDevicesArrayAdapter
					.add(getString(R.string.noDevicePaired));
		}
	}

	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		unregisterReceiver(mReceiver);
	}

	/**
	 * Must be final!
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				tmpFoundDevicesList.clear();
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					// Checks to not insert duplicates into adapter
					if (!tmpFoundDevicesList.contains(device)) {
						tmpFoundDevicesList.add(device);
						String deviceInfo = device.getName() + "\n"
								+ device.getAddress();
						bluetoothDevicesArrayAdapter.add(deviceInfo);
					}
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				// TODO this was creating a loop!
				// mBluetoothAdapter.cancelDiscovery();
				if (bluetoothDevicesArrayAdapter.isEmpty()) {
					bluetoothDevicesArrayAdapter.add(getString(R.string.noDeviceFound));
				}
				// Indicate scanning in the title
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.activity_bluetooth_devices_title);
				btnScan.setVisibility(View.VISIBLE);

			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_OFF) {
					Toast.makeText(getApplicationContext(),
							"Bluetooth turned off", Toast.LENGTH_SHORT).show();
					setResultCode(RESULT_CANCELED);
					finish();
				}
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
					Toast.makeText(getApplicationContext(),
							"Bluetooth turned ON", Toast.LENGTH_SHORT).show();
				}
			}

		}
	};

	/**
	 * This function registers our BroadcastReceiver receiver to following
	 * IntentFilters.
	 */
	public void registerReceiver() {
		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(mReceiver, filter);
		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				Toast.makeText(getApplicationContext(),
						"Turning ON Bluetooth...", Toast.LENGTH_SHORT).show();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Bluetooth is required.",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.connected_menu, menu);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int arg2,
			long arg3) {
		// Cancel discovery because it's costly and we're about to connect
		mBluetoothAdapter.cancelDiscovery();

		String auxString = ((TextView) view).getText().toString();
		// TODO
		// String auxString =
		// bluetoothDevicesArrayAdapter.getItem(arg2).toString();
		// if (adapterView.getId() == R.id.listViewDevices) {
		if (auxString != getString(R.string.noDeviceFound)
				&& auxString != getString(R.string.noDevicePaired)) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
			
			String deviceNameDeviceGroup = auxString.substring(0, auxString.length()
					- Constants.MAC_ADDRESS_CHAR_LENGTH - 1);
			String names[] = MathUtil.getDeviceNamesToReturn(deviceNameDeviceGroup);
			String address = auxString.substring(auxString.length()
					- Constants.MAC_ADDRESS_CHAR_LENGTH);
			

			Intent intentData = new Intent();
			intentData.putExtra(EXTRA_DEVICE_ADDRESS, address);
			intentData.putExtra(EXTRA_DEVICE_NAME, names[0]);
			intentData.putExtra(EXTRA_DEVICE_GROUP_NAME, names[1]);

			setResult(Activity.RESULT_OK, intentData);

			finish();
		}

	}

}
