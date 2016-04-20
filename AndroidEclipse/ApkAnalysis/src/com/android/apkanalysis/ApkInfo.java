package com.android.apkanalysis;

public class ApkInfo {

	private String mLabel;
	private String mPackageName;
	private String mProcessName;

	public ApkInfo(String label, String packageName, String processName) {
		mLabel = label;
		mPackageName = packageName;
		mProcessName = (processName == null ? packageName : processName);
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

	@Override
	public String toString() {
		return "lable=" + mLabel + ", packageName=" + mPackageName + ", processName=" + mProcessName;
	}
}
