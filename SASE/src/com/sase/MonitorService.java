package com.sase;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

public class MonitorService extends Service  implements SensorEventListener {
	public static final String PREFS_NAME = "SASEPrefs";
	SharedPreferences settings;// = getSharedPreferences(PREFS_NAME, 0);
	SharedPreferences.Editor editor;// = settings.edit();
	LocationManager location;
	SASELocListener locListener;
	bluetoothReceiver br;
	String TAG = "SASE";
	static final int number_of_threads = 4;
	static final int BT_THREAD = 0;
	static final int NFC_THREAD = 1;
	static final int PROX_THREAD = 2;
	static final int LOCN_THREAD = 3;
	
	Thread []threads;
	Thread master; 
	
	String intentdata[];
	int intentdata_as_int = 0;
	static final int REQUEST_ENABLE_BT = 0;
	boolean bluetoothFound = false;
	List<String> sharedList = new ArrayList<String>(); 
	
	public void initializeList() {
		for (int i=0; i<number_of_threads; i++)
			sharedList.add(i, new String(""));
	}
	
	public class NFCReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//
			Log.v(TAG, "GOTIT!" + intent);
		}
	}
	
	class monitorThread implements Runnable {

		int priorValue = 1;
		int currentValue = 1;
		@Override
		public void run() {
			Log.v(TAG, "master thread started: TID-"+Thread.currentThread().getId());
			// TODO Auto-generated method stub
			while (true) {
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.v(TAG, "master thread finished sleeping");
				int len = sharedList.size();
				Log.v(TAG, "DEBUG");
				if(len == 0)
					continue;
				Log.v(TAG, "DEBUG");
				String values[] = new String[len];
				Log.v(TAG, "DEBUG");
				for (int i=0; i<len; i++) {
					values[i] = sharedList.get(i).toString();
				}
				Log.v(TAG, "master thread checking policy");
				currentValue = checkPolicy(values);
				if(settings.getInt("alertLevel", -5) == 0) {
					sendAlarm(getApplicationContext(), 1);
					editor.putInt("alertLevel", 1);
				}
				else if(currentValue != priorValue) {
					Log.v(TAG, "master thread: Firing alarm level : " + Integer.toString(currentValue));
					sendAlarm(getApplicationContext(), currentValue);
					priorValue = settings.getInt("alertLevel", -5);
					editor.putInt("alertLevel", currentValue);
				}
				else {
					Log.v(TAG, "master thread: Alarm level unchanged");
				}
				Log.v(TAG, "master thread: master thread restarting");
			}
		}
	}
	
	int checkPolicy (String values[]) {
		for(String temp: values) {
			Log.v(TAG, temp + "....");
			if(temp.equals("found")) {
				return 1;
			}
		}
		return 5;
	}
	
	static void sendAlarm(Context context, int value) {
		Intent serviceThruBroadcast = new Intent();
		serviceThruBroadcast.setAction("com.sase.policy_trigger");
		serviceThruBroadcast.putExtra("AlarmLevel", value);
		context.sendBroadcast(serviceThruBroadcast);
	}
	
	class bluetoothReceiver extends BroadcastReceiver {

		String bluetoothKeyAddress = "00:24:04:45:9A:7B";
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			BluetoothDevice bd = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Log.v(TAG, "Device found: "+bd.getName()+". Address: "+bd.getAddress());
			if(bd.getAddress().equals(bluetoothKeyAddress)) {
				bluetoothFound = true;
			}
		}
		
	};
	
	class NFCThread implements Runnable {
		@Override
		public void run() {
			
		}
	};
	
	NdefMessage[] getNdefMessages(Intent intent) {
	    // Parse the intent
	    NdefMessage[] msgs = null;
	    String action = intent.getAction();
	    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
	        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	        if (rawMsgs != null) {
	            msgs = new NdefMessage[rawMsgs.length];
	            for (int i = 0; i < rawMsgs.length; i++) {
	                msgs[i] = (NdefMessage) rawMsgs[i];
	            }
	        }
	        else {
	        // Unknown tag type
	            byte[] empty = new byte[] {};
	            NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
	            NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
	            msgs = new NdefMessage[] {msg};
	        }
	    }        
	    else {
	        Log.e(TAG, "Unknown intent " + intent);
	    }
	    return msgs;
	}
	
	class BluetoothThread implements Runnable
	{
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		@Override
		public void run() {
			while (true) {
				mBluetoothAdapter.startDiscovery();
				
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					
				}
				if (bluetoothFound == true) {
					Log.v(TAG, "Found the key");
					sharedList.set(BT_THREAD, "found");
				}
				else {
					Log.v(TAG, "Key not found ");
					sharedList.set(BT_THREAD, "");
				}
				bluetoothFound = false;
			}
		}
	};
	
	class LocationThread implements Runnable
	{

		@Override
		public void run() {
			MonitorService.this.checkLocation();
		}
		
	};
	
	class ProximityThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			MonitorService.this.checkProximity();
		}
		
	};
	
	private SensorManager SenseManager;
    private Sensor ProximitySensor;

    SensorEvent event;

		
	class LocationHandler extends Handler {
		
	    @Override
	    public void handleMessage(Message msg) {
	    	MonitorService.this.checkLocation();
	    }
	    
	    public void sleep(long delayMillis) {
	      this.removeMessages(0);
	      sendMessageDelayed(obtainMessage(0), delayMillis);
	    }
	};
	
	public class SASELocListener implements LocationListener 
	{
		String locationstuff = "UNINITIALIZED";
		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			locationstuff = "Latitude: " + location.getLatitude()+"\nLongitude: "+location.getLongitude() + "\nProvider: "+ location.getProvider(); 
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			locationstuff = "GPS Disabled";
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			locationstuff = "GPS Enabled";
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
		public String getLocStuff()
		{
			return locationstuff;
		}
		
	
	}
	
	public void checkLocation()
	{
		while (true) {
			
			intentdata[0] = locListener.getLocStuff();
			Intent serviceThruBroadcast = new Intent();
			serviceThruBroadcast.setAction("com.sase.policy_trigger");
			serviceThruBroadcast.putExtra("AlarmLevel", intentdata);
			this.sendBroadcast(serviceThruBroadcast);
			try {
				  Thread.sleep(1000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		handle.sleep(5000);
	}
	

	          
	@Override
	public void onCreate(){
		settings = getSharedPreferences(PREFS_NAME, 0);
		editor = settings.edit();
		editor.putBoolean("active", true);
		editor.putInt("alertLevel", -1);
		
	      // Commit the edits!
		editor.commit();
		
		initializeList();
    	SenseManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    	ProximitySensor = SenseManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    	SenseManager.registerListener(this, ProximitySensor, SensorManager.SENSOR_DELAY_UI);
		intentdata = new String[2];
		intentdata[0] = intentdata[1] = "UNINITIALIZED";
		
		threads = new Thread[number_of_threads] ;
		threads[BT_THREAD] = new Thread(new BluetoothThread());
		threads[PROX_THREAD] = new Thread(new ProximityThread());
		threads[LOCN_THREAD] = new Thread(new LocationThread());
		master = new Thread(new monitorThread());
		try {
//			handle = new LocationHandler();
//			t1.start();
//			t2.start();
//			proximity = new ProximityHandler();
			threads[BT_THREAD].start();
			master.start();
//			threads[PROX_THREAD].start();
//			threads[LOCN_THREAD].start();
			location = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			locListener = new SASELocListener();
		}
		catch (Exception e)
		{
			Toast.makeText(this, "FAIL", Toast.LENGTH_SHORT);
		}
		Log.d(TAG, "Service started?");
		Context context = getApplicationContext();
		CharSequence text = "Service Created!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
//		showToastGPS();
//		checkProximity();
		
		location.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
		
		br = new bluetoothReceiver();
		this.registerReceiver(br, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
	}
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	return START_STICKY;
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
    @Override
    public void onDestroy() {
    	Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    	Process.killProcess(Process.myPid());
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		this.event = event;
	}

	public void checkProximity()
	{
		while (true) {
			try {
				  Thread.sleep(1000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(event.values[0] == 5.0)
			{
				intentdata[1] = "far";
				intentdata_as_int = 5;
			}
			else if(event.values[0] == 0.0)
			{
				intentdata[1] = "close";
				intentdata_as_int = 0;
				//wifi;
			}
			else
				intentdata[1] = "New Value";
			Intent serviceThruBroadcast = new Intent();
			serviceThruBroadcast.setAction("com.sase.policy_trigger");
			serviceThruBroadcast.putExtra("AlarmLevel", intentdata_as_int);
			this.sendBroadcast(serviceThruBroadcast);
			if(intentdata_as_int == 0) {
				try {
					  Thread.sleep(5000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}


