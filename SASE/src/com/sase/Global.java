package com.sase;

import android.app.Application;

public class Global extends Application{
	private static boolean active = false;
	
	public boolean getServiceState() {
		return active;
	}
	
	public void setServiceState(boolean state) {
		active = state;
	}
	
}