package com.android.apkanalysis;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public class ApkProcessInfo {

	private String mName;
	private String mPackageName;
	private ArrayList<String> mProcessList;

	public ApkProcessInfo(String name, String packageName) {
		mName = name;
		mPackageName = packageName;
		mProcessList = new ArrayList<String>();
	}

	public String getLabel() {
		return mName;
	}

	public String getPackageName() {
		return mPackageName;
	}
	
	public ArrayList<String> getProcessName() {
		return mProcessList;
	}
	
	public void addProcess(String process) {
		if (!TextUtils.isEmpty(process) && !isApkProcess(process)) {
			mProcessList.add(process);
		}
	}
	
	public void removeProcess(String process) {
		if (!TextUtils.isEmpty(process) && isApkProcess(process)) {
			mProcessList.remove(process);
		}
	}
	
	public void setProcessList(ArrayList<String> processes) {
		mProcessList = processes;
	}
	
	public boolean isApkProcess(String process) {
		boolean result = false;
		if (!TextUtils.isEmpty(process)) {
			for (int i = 0; i < mProcessList.size(); i++) {
				if (mProcessList.get(i).equals(process)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "lable=" + mName + ", packageName=" + mPackageName + ", processList=" + getProcessListString();
	}

	private String getProcessListString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		for (int i = 0; i < mProcessList.size(); i++) {
			sb.append(mProcessList.get(i));
			if (i + 1 < mProcessList.size()) {
				sb.append(", ");
			}
		}
		sb.append(" }");
		return sb.toString();
	}
}
