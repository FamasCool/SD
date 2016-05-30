package com.qty.cpuusage;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridLayout;

public class CpuUsagePreference extends Preference {
	
	private static final boolean DEBUG = true;
	private static final String TAG = "CpuUsagePreference";
	
	private GridLayout mCpuUsageContainer;
	private CpuUsageView[] mCpuUsageViews;
	
	public CpuUsagePreference(Context context) {
		this(context, null);
	}
	
	public CpuUsagePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuUsagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	@Override
	protected void onBindView(View view) {
		mCpuUsageContainer = (GridLayout) view.findViewById(R.id.gird_containt);
		int core = CpuUtils.getCpuCore();
		if (DEBUG) Log.d(TAG, "onBindView=>core: " + core);
		if (core > 0) {
			mCpuUsageViews = new CpuUsageView[core];
			for (int i = 0; i < mCpuUsageViews.length; i++) {
				mCpuUsageViews[i] = new CpuUsageView(getContext());
				LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				mCpuUsageViews[i].setLayoutParams(lp);
				mCpuUsageViews[i].setCore(i);
				if (DEBUG) Log.d(TAG, "onBindView=>i: " + i);
				mCpuUsageContainer.addView(mCpuUsageViews[i]);
			}
		}
		
		StringBuilder summary = new StringBuilder();
		if (core > 0) {
			summary.append(getCpuCoreDiscription(core));
		}
		long freq = CpuUtils.getMaxCpuFreq();
		if (DEBUG) Log.d(TAG, "onBindView=>freq: " + freq);
		if (freq > 0) {
			if (summary.length() > 0) {
				summary.append("\n");
			}
			summary.append(CpuUtils.formatFrequency(freq));
		}
		setSummary(summary.toString());
		super.onBindView(view);
	}
	
	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		if (DEBUG) Log.d(TAG, "onAttachedToHierarchy()...");
	}
	
	@Override
	protected void onPrepareForRemoval() {
		super.onPrepareForRemoval();
		if (DEBUG) Log.d(TAG, "onPrepareForRemoval()...");
	}
	
	private String getCpuCoreDiscription(int core) {
		Resources res = getContext().getResources();
		switch (core) {
		case 1:
			return res.getString(R.string.single_core);
			
		case 2:
			return res.getString(R.string.double_core);
			
		case 4:
			return res.getString(R.string.four_core);
			
		case 8:
			return res.getString(R.string.eight_core);
			
		default:
			return res.getString(R.string.four_core);
		}
	}

}
