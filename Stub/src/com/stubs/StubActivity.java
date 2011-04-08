package com.stubs;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class StubActivity extends Activity {
    /** Called when the activity is first created. */
	TextView t;
	// Broadcast Receiver that displays the intent
	class ActivityReceiver extends BroadcastReceiver {
		
		TextView t;
		
		// Pass the textview so that the text can be set 
		ActivityReceiver(TextView temp) {
			t = temp;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			// Grab intent temp data and display it on screen as an integer
			String temp = Integer.toString(intent.getIntExtra("AlarmLevel", 0));
			t.setText("Received an intent\n\nIntent as raw string: \n" + intent.toString() + "\n\nAlert Level: "+temp);
			t.setText(t.getText() + "\n" + getPackageManager().getNameForUid(6824));
			
		}
		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.v("StubActivity", "Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        t = (TextView) findViewById(R.id.status_text);
        
        // Declare and initialize the BroadcastReceiver
        ActivityReceiver ar = new ActivityReceiver(t);
        
        t.setText("Waiting for intent to be triggered..");
        
        // Register the BroadcastReceiver and a filter
        IntentFilter filter = new IntentFilter();
	    filter.addAction("com.sase.policy_trigger");
	    this.registerReceiver(ar, new IntentFilter("com.sase.policy_trigger"));
	    
	    Button button = (Button) findViewById(R.id.button1);
	    final EditText intentValue = (EditText)findViewById(R.id.editText1);
	    intentValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus == true)
					intentValue.setText("");
				
			}
		});
	    button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent broadcast = new Intent();
                
                broadcast.setAction("com.sase.policy_trigger");
                if(Integer.parseInt(intentValue.getText().toString()) >= 0 && Integer.parseInt(intentValue.getText().toString()) <= 5) {
                	broadcast.putExtra("AlarmLevel", Integer.parseInt(intentValue.getText().toString()));
                	getApplicationContext().sendBroadcast(broadcast);
                }
            }
        });
    }
}
