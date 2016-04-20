package com.android.apkanalysis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(this, "onReceive=>action: " + action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			Intent service = new Intent(context, ApkAnalysisService.class);
			context.startService(service);
		}
	}

}
