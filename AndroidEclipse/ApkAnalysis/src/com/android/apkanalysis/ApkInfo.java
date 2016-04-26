package com.android.apkanalysis;

import android.graphics.drawable.Drawable;

public class ApkInfo {

	private Drawable mIcon;
	private String mLabel;
	private String mPackageName;
	private String mProcessName;
	private boolean mIsChecked;

	public ApkInfo(Drawable icon, String label, String packageName, String processName, boolean isChecked) {
		mIcon = icon;
		mLabel = label;
		mPackageName = packageName;
		mProcessName = (processName == null ? packageName : processName);
		mIsChecked = isChecked;
	}

	public String getLabel() {
		return mLabel;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getProcessName() {
		return mProcessName;
	}

	public Drawable getIcon() {
		return mIcon;
	}

	public boolean isChecked() {
		return mIsChecked;
	}
	
	public void setChecked(boolean checked) {
		mIsChecked = checked;
	}

	@Override
	public String toString() {
		return "lable=" + mLabel + ", packageName=" + mPackageName + ", processName=" + mProcessName + ", checked="
				+ mIsChecked;
	}
}
