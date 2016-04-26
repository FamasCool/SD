package com.android.apkanalysis;
//PID PR CPU% S  #THR     VSS     RSS PCY UID      Name

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AnalysisInfo {

	private String mPackageName;
	private int mCPU;
	private long mPSS;
	private long mTime;

	public AnalysisInfo(String packageName, int cpu, long pss, long time) {
		mPackageName = packageName;
		mCPU = cpu;
		mPSS = pss;
		mTime = time;
	}

	public AnalysisInfo(String packageName, long time) {
		mPackageName = packageName;
		mCPU = 0;
		mPSS = 0;
		mTime = time;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public void increaseCpu(int cpu) {
		mCPU += cpu;
	}

	public int getCpu() {
		return mCPU;
	}

	public void increaseMemory(long pss) {
		mPSS += pss;
	}

	public long getPss() {
		return mPSS;
	}

	public long getTime() {
		return mTime;
	}

	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return "packageName=" + mPackageName + ", CPU=" + mCPU + ", PSS=" + mPSS + ", time="
				+ sdf.format(new Date(mTime));
	}
}
