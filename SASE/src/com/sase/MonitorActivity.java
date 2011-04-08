package com.sase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MonitorActivity extends Activity {
	public static final String PREFS_NAME = "SASEPrefs";
	private OnClickListener ServiceStarter = new OnClickListener() {
	    public void onClick(View v) {
			Context context = getApplicationContext();
			CharSequence text = "Creating Service";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			//toast.show();
			Intent serviceStart =new Intent(MonitorActivity.this, com.sase.MonitorService.class);
			//serviceStart.setClassName("com.sase", "com.sase.MonitorService");
			//Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost/"));
			//startService(serviceStart);
			Intent serviceThruBroadcast = new Intent();
			serviceThruBroadcast.setAction("com.sase.SERVICE_START");
			context.sendBroadcast(serviceThruBroadcast);
			TextView textView = (TextView)findViewById(R.id.hello_world_text);
			textView.setText("Service Started");
	      // do something when the button is clicked
	    }
	};
	
	private OnClickListener ServiceStopper = new OnClickListener() {
	    public void onClick(View v) { 
	    	Context context = getApplicationContext();
			CharSequence text = "Stopping Service";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			Intent serviceThruBroadcast = new Intent();
			serviceThruBroadcast.setAction("com.sase.SERVICE_KILL");
			context.sendBroadcast(serviceThruBroadcast);
			TextView textView = (TextView)findViewById(R.id.hello_world_text);
			textView.setText("Service Stopped");
	    	
	    }
	};
	@Override
	public void onCreate(Bundle savedInstance)
	{
		if(getIntent().getAction().equals("android.nfc.action.NDEF_DISCOVERED") == true) {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			if(settings.getBoolean("active", false) == true) {
				MonitorService.sendAlarm(getApplicationContext(), 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("alertLevel", 0);
			}
			finish();
		}
		super.onCreate(savedInstance);
		setContentView(R.layout.main);
		Button startButton = (Button)findViewById(R.id.button1);
		startButton.setOnClickListener(ServiceStarter);
		TextView GPSStatus = (TextView) findViewById(R.id.textView1);
		//GPSStatus.setText("No Exception!");
		Button stopButton = (Button)findViewById(R.id.button2);
		stopButton.setOnClickListener(ServiceStopper);

//		for(int i = 0; i< providers.size(); i++)
//		{
//			a = a+providers.get(i);
//		}
//		GPSStatus.setText(a);
	}
}