package com.android.apkanalysis;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ApkAnalysisActivity extends Activity implements OnClickListener {
	
	private Button mStart;
	private Button mStop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_apk_analysis);
		mStart = (Button) findViewById(R.id.start);
		mStop = (Button) findViewById(R.id.stop);
		
		mStart.setOnClickListener(this);
		mStop.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateButtonState();
	}

	@Override
	public void onClick(View v) {
		Intent service = new Intent(this, ApkAnalysisService.class);
		switch (v.getId()) {
		case R.id.start:
			startService(service);
			mStart.setEnabled(false);
			mStop.setEnabled(true);
			break;
			
		case R.id.stop:
			stopService(service);
			mStart.setEnabled(true);
			mStop.setEnabled(false);
			break;
		}
	}
	
	private void updateButtonState() {
		if (isServiceRunning()) {
			mStart.setEnabled(false);
			mStop.setEnabled(true);
		} else {
			mStart.setEnabled(true);
			mStop.setEnabled(false);
		}
	}
	
	private boolean isServiceRunning() {
		boolean result = false;
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  
	    List<RunningServiceInfo> list = am.getRunningServices(50);  
	    if (list.size() <= 0) {  
	        return false;  
	    }  
	    for (int i = 0; i < list.size(); i++) {  
	        String name = list.get(i).service.getClassName().toString(); 
	        //Log.d(this, "isServiceRunning=>name: " + name + " service: " + ApkAnalysisService.class.getName());
	        if (name.equals(ApkAnalysisService.class.getName())) {  
	        	result = true;  
	            break;  
	        }  
	    }  
		return result;
	}

}
