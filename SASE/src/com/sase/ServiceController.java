package com.sase;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ServiceController extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.sase.SERVICE_KILL"))
        {
        	Toast.makeText(context, "Got the kill request", Toast.LENGTH_SHORT).show();
        	Intent temp = new Intent();
        	temp.setAction("com.sase.MonitorService");
            context.stopService(temp);
        }
        else if(intent.getAction().equals("com.sase.SERVICE_START"))
        {
        	Toast.makeText(context, "Got the start request", Toast.LENGTH_SHORT).show();
        	Intent temp = new Intent();
        	temp.setAction("com.sase.MonitorService");
        	//Log.v("ServiceController", temp.toString()+ " was the intent I sent");
        	//Log.d("ServiceController", "Sent the service start intent.");
            context.startService(temp);
        }
    }
}