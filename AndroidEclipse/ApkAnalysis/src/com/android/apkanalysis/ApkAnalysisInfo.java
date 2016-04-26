package com.android.apkanalysis;

public class ApkAnalysisInfo {

	private String mApkName;
	private int mMaxCpuUsage;
	private int mAveCpuUsage;
	private long mMaxMemoryUsage;
	private long mAveMemoryUsage;
	private int mCpuTimes;
	private int mMemoryTimes;
	private long mRecordTime;

	public ApkAnalysisInfo(String name, int cpuMax, int cpuAve, long memoryMax, long memoryAve,
			int cpuTimes, int memoryTimes, long time) {
		mApkName = name;
		mMaxCpuUsage = cpuMax;
		mAveCpuUsage = cpuAve;
		mMaxMemoryUsage = memoryMax;
		mAveMemoryUsage = memoryAve;
		mCpuTimes = cpuTimes;
		mMemoryTimes = memoryTimes;
		mRecordTime = time;
	}
	
	public String getName() {
		return mApkName;
	}
	
	public int getMaxCpuUsage() {
		return mMaxCpuUsage;
	}
	
	public int getAveCpuUsage() {
		return mAveCpuUsage;
	}
	
	public long getMaxMemoryUsage() {
		return mMaxMemoryUsage;
	}
	
	public long getAveMemoryUsage() {
		return mAveMemoryUsage;
	}
	
	public int getCpuTimes() {
		return mCpuTimes;
	}
	
	public int getMemoryTimes() {
		return mMemoryTimes;
	}
	
	public long getTime() {
		return mRecordTime;
	}
	
	@Override
	public String toString() {
		return "name=" + mApkName + ", cpuMax=" + mMaxCpuUsage + ", cpuAve=" + mAveCpuUsage + ", memoryMax="
				+ mMaxMemoryUsage + ", memoryAve=" + mAveMemoryUsage + ", times=" + mCpuTimes + ", memorytimes=" + mMemoryTimes + ", time="
				+ mRecordTime;
	}

}
