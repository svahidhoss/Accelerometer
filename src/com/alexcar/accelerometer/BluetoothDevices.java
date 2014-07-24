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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class BluetoothDevices extends Activity implements OnItemClickListener {

	
	private static final int REQUEST_ENABLE_BT = 1;

	static final String EXTRA_ADDRESS = "resultActivityExtraAddress";

	public static String address = "";

	
	BluetoothAdapter mBluetoothAdapter;
	Set<BluetoothDevice> pairedDevices;
	ArrayAdapter<String> mArrayAdapter=null;
	
	BroadcastReceiver mReceiver;
	Button scanButton;
	TextView textViewLabel;
	ProgressBar progressBar;
	
	ListView listaViewDevices;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_bluetooth_devices);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		initViews();
		initBluetooth();
		
		

		
		if (mBluetoothAdapter.isDiscovering()){
			mBluetoothAdapter.cancelDiscovery();
		}
	}



	private void initViews() {
		// TODO Auto-generated method stub
		progressBar = (ProgressBar) findViewById(R.id.progressBarScaning);
		progressBar.setVisibility(View.GONE);
		
		listaViewDevices = (ListView) findViewById(R.id.listViewDevices);
		listaViewDevices.setOnItemClickListener(this);
		
		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
		listaViewDevices.setAdapter(mArrayAdapter);
		textViewLabel= (TextView) findViewById(R.id.textViewLabel);
		textViewLabel.setText("Pareid Devices:");
		scanButton = (Button) findViewById(R.id.buttonScan);
		scanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				scanButton.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				textViewLabel.setText("Scaning for more devices...");
				mArrayAdapter.clear();
				initBluetooth();
				mBluetoothAdapter.startDiscovery();
			}
		});
	}

	
	
	
	private void initBluetooth() {
		// TODO Auto-generated method stub
		
		
		if (mBluetoothAdapter==null){
			Toast.makeText(getApplicationContext(), "No Bluetooth Available", Toast.LENGTH_SHORT).show();
			finish();
		}else{
			//Toast.makeText(this, "you have Bluetooth. Ok", Toast.LENGTH_SHORT).show();
		}
		
		if (!mBluetoothAdapter.isEnabled()){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //constante para reconocer esta actividadd en onActivityResult()

		}
		
		pairedDevices=mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size()>0){
			for (BluetoothDevice device:pairedDevices){
				mArrayAdapter.add(device.getName()+"\n"+device.getAddress());
			}
		}
		
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				
				if (BluetoothDevice.ACTION_FOUND.equals(action)){
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					String str = device.getName()+"\n"+device.getAddress();
					for (int i=0;i<mArrayAdapter.getCount();i++){
						
						if (mArrayAdapter.getItem(i).equals(str)){
							break;
						}else if(i==mArrayAdapter.getCount()-1){
							mArrayAdapter.add(str);	
							
						}
					}
					
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
					mBluetoothAdapter.cancelDiscovery();
					if (!mArrayAdapter.isEmpty()){
						textViewLabel.setText("Available devices:");
					}else{
						textViewLabel.setText("There is no Devices detected!");
					}
					progressBar.setVisibility(View.GONE);
					scanButton.setVisibility(View.VISIBLE);
					//Toast.makeText(getApplicationContext(), "fin Discovering", Toast.LENGTH_SHORT).show();
					
				}else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
					
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)==BluetoothAdapter.STATE_OFF){
						Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_SHORT).show();
						setResultCode(RESULT_CANCELED);
						finish();
						
					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)==BluetoothAdapter.STATE_ON){
						Toast.makeText(getApplicationContext(), "Bluetooth turned ON", Toast.LENGTH_SHORT).show();
						
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
		// TODO Auto-generated method stub
		super.onPause();
		if (mBluetoothAdapter.isDiscovering()){
			mBluetoothAdapter.cancelDiscovery();
			
		}
		if (mReceiver!=null){
			unregisterReceiver(mReceiver);
			
		}
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Intent data = new Intent();
		data.putExtra(EXTRA_ADDRESS, address);
		
		setResult(Activity.RESULT_CANCELED, data);
		

		finish();
		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==REQUEST_ENABLE_BT){
			if (resultCode==RESULT_OK){
				//Toast.makeText(getApplicationContext(), "Turning ON Bluetooth...", Toast.LENGTH_SHORT).show();
			}else if (resultCode==RESULT_CANCELED){
				Toast.makeText(this, "Bluetooth is required.", Toast.LENGTH_SHORT).show();
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
		if (arg0.getId()==R.id.listViewDevices){
			if (mBluetoothAdapter.isDiscovering()){
				mBluetoothAdapter.cancelDiscovery();
				
			}
			String auxString=mArrayAdapter.getItem(arg2).toString();
			
			
			address = auxString.substring(auxString.length() - 17);
			//Toast.makeText(this, "address: "+address, Toast.LENGTH_SHORT).show();

			Intent data = new Intent();
			data.putExtra(EXTRA_ADDRESS, address);
	
			setResult(Activity.RESULT_OK, data);

			finish();

            
		
		}
		
	}
	
	
}
