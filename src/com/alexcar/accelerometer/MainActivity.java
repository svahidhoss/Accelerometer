package com.alexcar.accelerometer;

import java.util.Calendar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int GET_ADDRESS = 2;
	
	
	//*****references********
	mMath mmath = new mMath();
	Constant constants = new Constant();
	//*****end references*****
	
	
	//********bluetooth*****
	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	BroadcastReceiver mReceiver;
	ConnectThread ct=null;
	ConnectedThread connected=null;
	Button bConnect;
	String nameDevice="";
	//********end bluetooth****
	
	//********HANDLER*********************** we use this handler to enable communication with the threads. 
	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			
			if (msg.what==Constant.CONNECTED_HANDLER){
				//Toast.makeText(getApplicationContext(), "Connected! (handler info)", Toast.LENGTH_SHORT).show();
				connected();
			}else if (msg.what==Constant.CONNECTING_HANDLER){
				TextView state = (TextView)findViewById(R.id.textViewNotConnected);
				state.setText("Connecting...");	
			}else if(msg.what==Constant.DISCONNECTED_HANDLER){
				Toast.makeText(getApplicationContext(), "Unable to connect!", Toast.LENGTH_SHORT).show();
				notConnected();
			}
		}
	};
	//*****end HANDLER********************
	
	//******sensors*****
	SensorManager mSensorManager;
	Sensor mAccelerometer, mOrientation;
	
		//----values---
	float[] acceleromterValues = new float[]{0,0,0};   //it's important to initialize the values.
	float[] orientationValues = new float[]{0,0,0};
		//-end--values-
	
	
		//---filters--
	
	boolean brOn=false;  //on when is more than one minimum defined (Constant.precision)
	boolean brReal=false;  //when the braking is more long than (Constant.marginMilliseconds)
	Calendar brTimeIni=null;
	
		//****calculate angles average
	boolean noise = false;
	boolean onAngles = false;
	float sum_angles1 = 0f;
	float sum_angles1_aux = 0f;
	float sum_angles2 = 0f;
	float sum_angles2_aux = 0f;
	int n = 0;
	int n_aux = 0;
	float[] orientationValuesAnterior = new float[]{0,0,0};
	
		//*****end*angles average
	
	
	
		//-end--filters--
	
	//*****end sensors*****

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//****
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //keep the screen on (check)
		//****
		
		//setContentView(R.layout.not_connected);
		
		//******
		notConnected();		//include setContentView, listener for the button, state, etc
		
		initializeBluetooth();		//include turn on, find devices with BluetoothDevices.class, registerReceivers, 
		//******
		
		
	
		
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		notConnected();  //#check, ... I put this here because when the orientation changes, the Activity is desroyed, so the device is disconnecting automatically (cancelAll())...
		unregisterSensores();
		finilizeAll();
	}



	private void initializeBluetooth() {
		// TODO Auto-generated method stub
		
	
		if (mBluetoothAdapter==null){
			Toast.makeText(getApplicationContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
			notConnected();
			return;
			
			
		}else if (!mBluetoothAdapter.isEnabled()){
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);   //if the user accept, onActivityResutl calls getDeviceAddressAndConnect();
		}else{
			getDeviceAddressAndConnect();		
		}
		
		
			
			
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
					
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0)==BluetoothAdapter.STATE_ON){
						Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_SHORT).show();
						notConnected();
					}
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)==BluetoothAdapter.STATE_ON){
						Toast.makeText(getApplicationContext(), "Bluetooth turned ON", Toast.LENGTH_SHORT).show();
						notConnected();
					}
					
				}else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){//check this //#//
					//we recieve this information also from the connectThread and the connectedThread.
					
					//Toast.makeText(getApplicationContext(), "Conexion was lost - broadcast", Toast.LENGTH_SHORT).show();
					//notConnected();
					//return;
				}else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
					//connected();						
				}
	
			}
		};
		IntentFilter filter;
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);  //
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		registerReceiver(mReceiver, filter);

			

	
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;

	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
			
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode==RESULT_OK){
				getDeviceAddressAndConnect();
			}else{
				Toast.makeText(getApplicationContext(), "Bluetooth is required", Toast.LENGTH_SHORT).show();
				notConnected();
				
			}
			break;
		case GET_ADDRESS:
			if (resultCode==RESULT_OK){
				String addressPasada = data.getExtras().getString(BluetoothDevices.EXTRA_ADDRESS);
				
				connect(addressPasada);
			}else{
				notConnected();
			}
		
		default:
			break;
		}
	}



	private void notConnected() {
		
		//*******
		setContentView(R.layout.not_connected);
		
		
		
		
		bConnect = (Button)findViewById(R.id.buttonConnectFromNoConnected);
		bConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				initializeBluetooth();
				
			}
		});
		
		TextView state = (TextView)findViewById(R.id.textViewNotConnected);
		

		//Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
		
		if (mBluetoothAdapter==null){
			bConnect.setVisibility(View.GONE);
			state.setText("Device does not support Bluetooth");
			ImageView imError = (ImageView)findViewById(R.id.imageViewWrong);
			imError.setVisibility(View.VISIBLE);
			//no tiene bluetooth posible, salir? texto?
		}else{
			bConnect.setVisibility(View.VISIBLE);
			
			if (!mBluetoothAdapter.isEnabled()){
				state.setText("Bluetooth is turned OFF");
				bConnect.setText(" Turn ON and Connect ");
			}else if (mBluetoothAdapter.isEnabled()){
				state.setText("Not Connected");
				bConnect.setText(" Connect Now ");
			}
		}

		
		unregisterSensores();	
	}

	private void getDeviceAddressAndConnect() {
		// TODO Auto-generated method stub
		startActivityForResult(new Intent(this, BluetoothDevices.class), GET_ADDRESS);
	}
	
	
	private void connect(String address) {
		// TODO Auto-generated method stub
		
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		
		
		ct = new ConnectThread(device, mHandler);
		ct.start();	
		
		nameDevice=device.getName();
		//the receiver (mReceiver) is waiting for the signal of "device connected" to use the connection, and call connected(). 
			//connected() initialize also the ConnectedThread instance (- connected = new ConnectedThread(BleutoothSocket) -)
	}
	
	
	private void connected() {
		// TODO Auto-generated method stub

		Toast.makeText(getApplicationContext(), "Connected with "+nameDevice+"!", Toast.LENGTH_SHORT).show();
		
		setContentView(R.layout.activity_main);
		
		//*******
		BluetoothSocket mSocket = ct.getBTSocket();
		connected = new ConnectedThread(mSocket, mHandler);    //this instance of ConnectedThread is the one that we are going to use to write()
															//we don't need to start the Thread, because we are going to write, not to read() [write is not a blocking method]
		//*******

		
		
		/**
		 * we are ready to use the sensor and send the information of the brakings, so...
		 */
		//******SENSORS***********
		initializeSensors();
		//******END SENSORS*******
		
		
		
		
		//******example temp**********
		Button b = (Button) findViewById(R.id.button1);
		
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				connected.write(mmath.toByteArray(60));
				

				
			}
		});
		
		//********end**example******
		
		
		
	
		
		
		
	}
	
	private void initializeSensors() {
		
		//******sensors*****/
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);   //it is deprecated, but it works.
		//Toast.makeText(getApplicationContext(), "sensors created", Toast.LENGTH_SHORT).show();
		//
		//Register the sensors to the listener.
		  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		  mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
				
		//*****sensors (accelerometer, orientation)*******/
		
	}

	void unregisterSensores(){
		try {
			mSensorManager.unregisterListener(this);  //check!!! be sure that this work in wake_lock (permission to work with screen off)
			//Toast.makeText(getApplicationContext(), "sensor unregistered", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// TODO: handle exception
		}	
	}

	private void finilizeAll() {
		
		unregisterSensores();
		try {
			ct.cancel();
			//Toast.makeText(getApplicationContext(), "ct.cancel() (cancelAll())", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// TODO: handle exception
			//Toast.makeText(getApplicationContext(), "exception", Toast.LENGTH_SHORT).show();
		}
		
		try {
			connected.cancel();	
			//Toast.makeText(getApplicationContext(), "connected.cancel() (cancelAll())", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		try {
			this.unregisterReceiver(mReceiver);
			//Toast.makeText(getApplicationContext(), "receiver unregistered", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// TODO: handle exception
			
		}
		
	}
	
	
	
	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
	synchronized (this) {
		
		
		if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			
			acceleromterValues = mmath.cancelGravity(event.values, orientationValues);  //the sensor doesn't erase the gravity by itself
																							//for that exists other sensor: LINEAR_ACCELERATION, but is not very typical to have it.
			
			/*
			 * we dont need to cancel the gravity, because we are going to use the axes x0,y0,z0, in which angles the gravity are all it in z0
			 */ //but we do... the results appears to be better.
			
			//**acceleromterValues = event.values.clone();
			acceleromterValues = mmath.convertReference2(acceleromterValues, orientationValues);  //**
			
			
		//***/	double module = mmath.module(acceleromterValues[0], acceleromterValues[1], acceleromterValues[2]-SensorManager.GRAVITY_EARTH);
			//double module = mmath.module(acceleromterValues[0], acceleromterValues[1], event.values[2]-SensorManager.GRAVITY_EARTH);
			
			double module = mmath.module(acceleromterValues[0], acceleromterValues[1], 0);
			
		//*******first filter to brakings. 
			
			

			
			
			
			
			//*********braking????*********
			boolean braking=false;
			if (acceleromterValues[1]>0 && Math.abs(orientationValues[1])<90+Constant.precisionPitch){
				braking = true;
			}else if (acceleromterValues[1]<0 && Math.abs(orientationValues[1])>90+Constant.precisionPitch){
				braking = true;
			}
			
			boolean accelerating = false;
			if (acceleromterValues[1]<0 && Math.abs(orientationValues[1])<90){
				accelerating=true;
			}else if (acceleromterValues[1]>0 && Math.abs(orientationValues[1])>90){
				accelerating=true;
			}
			//******end***braking?????****
			

		//	if (module>Constant.precision && acceleromterValues[1]>0){
			if (module>Constant.precision && braking){ //braking is the boolean variable that defines the brake and the acceleration with the sign of y
				RelativeLayout background = (RelativeLayout)findViewById(R.id.connectedLayout);
				background.setBackgroundResource(R.color.dark_red);
				
				if (brOn==false){
					brOn=true;
					brReal=false;
					brTimeIni=Calendar.getInstance();
					
				}else{
					if (Calendar.getInstance().getTimeInMillis()-brTimeIni.getTimeInMillis()>Constant.marginMilliseconds){
						brReal=true;
						try {
							//connected.write((int) (10*module));	
							//connected.write(mmath.toByteArray(module));
							
						} catch (Exception e) {
							Toast.makeText(getApplicationContext(), "error writting", Toast.LENGTH_SHORT).show();
						}
						TextView tvaux = (TextView)findViewById(R.id.textViewMain);
						tvaux.setText(""+mmath.redondeo(module, 1));
						ProgressBar progress = (ProgressBar)findViewById(R.id.seekBar1);
						progress.setProgress((int) module);
						
					}else{
						brReal=false;
					}
					
				}
				
			}//end br starts
			else{
				//*****
				TextView tvaux = (TextView)findViewById(R.id.textViewMain);
				tvaux.setText("");
				//--
				ProgressBar progress = (ProgressBar)findViewById(R.id.seekBar1);
				progress.setProgress((int) (0));
				//*****
				brReal=false;
				brOn=false;
				RelativeLayout backg = (RelativeLayout)findViewById(R.id.connectedLayout);
				//if (acceleromterValues[1]<0 && module>Constant.precision){
				if (accelerating && module>Constant.precision){
					backg.setBackgroundResource(R.color.dark_green);
				}else{
					backg.setBackgroundColor(Color.WHITE);
					
				}
				
			}//end 'end brake'

			
		//******end the first filter to brakings
			
			/**
			 * ******WRITE TO DE BLUETOOTH DEVICE*********
			 */
			 
			//****writing also the module when brake is real.
			double moduleReal;
			if (brReal){
				moduleReal=module;
			}else{
				moduleReal=0;
				
			}
			
			
			
			//---with the idea of write this data and send by bluetooth, first it's necessary to pass them to byte...
			byte[] x = mmath.toByteArray(acceleromterValues[0]);
			byte[] y = mmath.toByteArray(acceleromterValues[1]);
			byte[] z = mmath.toByteArray(acceleromterValues[2]);
			byte[] mod_byte = mmath.toByteArray(module);
			byte[] xyz_and_Mod = new byte[8*4];
			
			xyz_and_Mod = mmath.concatenateBytes(mmath.concatenateBytes(mmath.concatenateBytes(x, y),z),mod_byte);
			//---	
			
			byte[] moduleRealByte = mmath.toByteArray(moduleReal);
			byte[] all = new byte[8*4+8];
			all = mmath.concatenateBytes(xyz_and_Mod, moduleRealByte);
			
			connected.write(all);
			//********write angles
				/*byte[] az = mmath.toByteArray(orientationValues[0]);
				byte[] pitch = mmath.toByteArray(orientationValues[1]);
				byte[] roll = mmath.toByteArray(orientationValues[2]);
				byte[] anglesByte = mmath.concatenateBytes(mmath.concatenateBytes(az, pitch), roll);
		
				connected.write(mmath.concatenateBytes(anglesByte, mod_byte));*/
			///****end write angles
			
			
			
			//*****end***writing also the module when brake is real.
			
			/**
			 * ******
			 */
			
			
			
		}// end case Accelerometer (if)
		
		
		
		else if (event.sensor.getType()==Sensor.TYPE_ORIENTATION){
			System.out.println("copy Orientation");
			String angles = "azimuth: "+mmath.redondeo(event.values[0], 3)+"\npitch: "+mmath.redondeo(event.values[1], 3)+"\nroll: "+mmath.redondeo(event.values[2], 3);
			TextView tv = (TextView) findViewById(R.id.textViewConnected);
			tv.setText(angles);	//see in the phone screen (debug)
			
			if (onAngles==false){
				orientationValues = event.values.clone();
				orientationValuesAnterior = event.values.clone();
				onAngles=true;
			}
			orientationValues[0]=event.values[0];
			
			//****calculate angles average
			
			float delta1 = event.values[1]-orientationValuesAnterior[1];
			float delta2 = event.values[2]-orientationValuesAnterior[2];
			float delta=Math.max(delta1, delta2);
					
			
			//.1
			if (delta < Constant.delta && noise==false){
				sum_angles1 = sum_angles1+event.values[1];
				sum_angles2 = sum_angles2+event.values[2];
				n++;
				orientationValues[1]=(float) sum_angles1/n;
				orientationValues[2]=(float) sum_angles2/n;
				if (n==Constant.nIter){
					
					sum_angles1=event.values[1];
					sum_angles2=event.values[2];
					n=1;
				}
			}else if (delta > Constant.delta){
				
				noise = true;
				sum_angles1_aux=0;
				n=0;
			}else if (delta < Constant.delta && noise==true){
				sum_angles1_aux=sum_angles1_aux+event.values[1];
				sum_angles2_aux=sum_angles2_aux+event.values[2];
				n++;
				if (n==Constant.nIter){
					orientationValues[1]=(float)sum_angles1_aux/n;
					orientationValues[2]=(float)sum_angles2_aux/n;
					
					sum_angles1=event.values[1];
					sum_angles2=event.values[2];
					n=1;
					noise=false;	
				}
			}
			
			//.2
			//****end***calculate angles average
			orientationValuesAnterior = event.values.clone();
		}//end case sensor Orientation
		
		
	}
	
	}//fin del syncronized this.  //#check
	
	public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch (item.getItemId()) {
		
	    	case R.id.action_settings:
	        	Intent intentSettings = new Intent(this, Settings_m.class);
				startActivity(intentSettings);
				return true;
	        default:
	            return super.onOptionsItemSelected(item);
    	}
    	
    }
	
	
}

