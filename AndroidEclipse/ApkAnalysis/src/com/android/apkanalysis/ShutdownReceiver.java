package com.android.apkanalysis;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class ShutdownReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(this, "onReceive=>action: " + action);
		if (Intent.ACTION_SHUTDOWN.equals(action)) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			long start = sp.getLong(Utils.START_TIME_KEY, -1);
			long stop = sp.getLong(Utils.STOP_TIME_KEY, -1);
			if (start != -1 && stop == -1) {
				Editor e = sp.edit();
				e.putLong(Utils.STOP_TIME_KEY, Calendar.getInstance().getTimeInMillis());
				e.commit();
			}
		}
	}

}
