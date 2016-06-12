package com.android.newfunstore;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NewsAndFunStore extends Activity implements OnClickListener {
	
	private static final String TAG = "NewsAndFunStore";
	
	private TextView mSmsNewsTv;
	private TextView mGphoneZoneTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_news_and_fun_store);
		ActionBar actionBar = getActionBar();
		//actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setTitle(R.string.news_and_fun_store_label);
		
		mSmsNewsTv = (TextView) findViewById(R.id.sms_news);
		mGphoneZoneTv = (TextView) findViewById(R.id.gphone_zone);
		
		mSmsNewsTv.setOnClickListener(this);
		mGphoneZoneTv.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick=>id: " + v.getId());
		switch (v.getId()) {
		case R.id.sms_news:
			Intent smsNews = new Intent(this, SmsNewsActivity.class);
			startActivity(smsNews);
			break;
			
		case R.id.gphone_zone:
			Uri gphoneZoneUri = Uri.parse(getString(R.string.gphone_zone_uri));
			Intent gphoneZone = new Intent(Intent.ACTION_VIEW, gphoneZoneUri);
			startActivity(gphoneZone);
			break;
		}
	}
	
	
}
