package com.android.newfunstore;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SmsNewsActivity extends Activity implements OnClickListener {
	
	private static final String TAG = "SmsNewsActivity";
	
	private static final int TYPE_CRICKET_NEWS_DIALOG = 1;
	private static final int TYPE_BREAKING_NEWS_DIALOG = 2;
	
	private TextView mCricketNewsTv;
	private TextView mBreakingNewsTv;
	
	private int mDialogType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_news);
		
		ActionBar actionBar = getActionBar();
		//actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		Button bt;
		
		mCricketNewsTv = (TextView) findViewById(R.id.cricket_news);
		mBreakingNewsTv = (TextView) findViewById(R.id.breaking_news);
		
		mCricketNewsTv.setOnClickListener(this);
		mBreakingNewsTv.setOnClickListener(this);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("SENT_SMS_ACTION");
		filter.addAction("DELIVERED_SMS_ACTION");
		registerReceiver(mSmsStatusReceiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mSmsStatusReceiver);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick=>id: " + v.getId());
		switch (v.getId()) {
		case R.id.cricket_news:
			mDialogType = TYPE_CRICKET_NEWS_DIALOG;
			break;
			
		case R.id.breaking_news:
			mDialogType = TYPE_BREAKING_NEWS_DIALOG;
			break;
		}
		showSmsNewsDialog(mDialogType);
	}
	
	private void showSmsNewsDialog(int type) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
		switch (type) {
		case TYPE_CRICKET_NEWS_DIALOG:
			builder.setTitle(R.string.cricket_news_dialog_title);
			builder.setMessage(R.string.cricket_news_dialog_message);
			break;
			
		case TYPE_BREAKING_NEWS_DIALOG:
			builder.setTitle(R.string.breaking_news_dialog_title);
			builder.setMessage(R.string.breaking_news_dialog_message);
			break;
		}
		builder.setPositiveButton(R.string.start, mDialogClickListener);
		builder.setNegativeButton(R.string.stop, mDialogClickListener);
		builder.create().show();
	}
	
	private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			Log.d(TAG, "onClick(Dialog)=>which: " + which);
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				switch (mDialogType) {
				case TYPE_CRICKET_NEWS_DIALOG:
					sendMessage(getString(R.string.cricket_news_number), getString(R.string.cricket_news_start_message));
					break;
					
				case TYPE_BREAKING_NEWS_DIALOG:
					sendMessage(getString(R.string.breaking_news_number), getString(R.string.breaking_news_start_message));
					break;
				}
				break;
				
			case DialogInterface.BUTTON_NEGATIVE:
				switch (mDialogType) {
				case TYPE_CRICKET_NEWS_DIALOG:
					sendMessage(getString(R.string.cricket_news_number), getString(R.string.cricket_news_stop_message));
					break;
					
				case TYPE_BREAKING_NEWS_DIALOG:
					sendMessage(getString(R.string.breaking_news_number), getString(R.string.breaking_news_stop_message));
					break;
				}
				break;
			}
		}
	};
	
	private void sendMessage(String number, String message) {
		SmsManager sm = SmsManager.getDefault();
		Intent deliverIntent = new Intent("SENT_SMS_ACTION");  
		Intent sendIntent = new Intent("DELIVERED_SMS_ACTION");
		PendingIntent deliverPI = PendingIntent.getBroadcast(this, 0,  
		       deliverIntent, 0);  
		PendingIntent sendPI = PendingIntent.getBroadcast(this, 0,  
				sendIntent, 0);  
		sm.sendTextMessage(number, null, message, sendPI, deliverPI);
	}
	
	private BroadcastReceiver mSmsStatusReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "onReceive=>action: " + action + " code: " + getResultCode());
			if ("SENT_SMS_ACTION".equals(action)) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
					break;
					
				default:
					Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show();
					break;
				}
			} else if ("DELIVERED_SMS_ACTION".equals(action)) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					//Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
					break;
					
				default:
					Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}
	};
	
}
