package com.android.apkanalysis;
//PID PR CPU% S  #THR     VSS     RSS PCY UID      Name

import java.util.ArrayList;

public class ProcessInfo {

	private String mPID;
	private String mPR;
	private String mCPU;
	private String mS;
	private String mTHR;
	private String mVSS;
	private String mRSS;
	private String mPCY;
	private String mUID;
	private String mName;

	public ProcessInfo(ArrayList<String> list) {
		if (list != null || list.size() == 10) {
			mPID = list.get(0);
			mPR = list.get(1);
			mCPU = list.get(2);
			mS = list.get(3);
			mTHR = list.get(4);
			mVSS = list.get(5);
			mRSS = list.get(6);
			mPCY = list.get(7);
			mUID = list.get(8);
			mName = list.get(9);
		} else {
			mPID = null;
			mPR = null;
			mCPU = null;
			mS = null;
			mTHR = null;
			mVSS = null;
			mRSS = null;
			mPCY = null;
			mUID = null;
			mName = null;
		}
	}

	public ProcessInfo(String pid, String pr, String cpu, String s, String thr, String vss, String rss, String pcy,
			String uid, String name) {
		mPID = pid;
		mPR = pr;
		mCPU = cpu;
		mS = s;
		mTHR = thr;
		mVSS = vss;
		mRSS = rss;
		mPCY = pcy;
		mUID = uid;
		mName = name;
	}

	public String getPid() {
		return mPID;
	}

	public String getPr() {
		return mPR;
	}

	public String getCpu() {
		return mCPU;
	}

	public String getS() {
		return mS;
	}

	public String getThr() {
		return mTHR;
	}

	public String getVss() {
		return mVSS;
	}

	public String getRss() {
		return mRSS;
	}

	public String getPcy() {
		return mPCY;
	}

	public String getUid() {
		return mUID;
	}

	public String getName() {
		return mName;
	}

	@Override
	public String toString() {
		return "PID=" + mPID + ", PR=" + mPR + ", CPU=" + mCPU + ", S=" + mS + ", THR=" + mTHR + ", VSS=" + mVSS
				+ ", RSS=" + mRSS + ", PCY=" + mPCY + ", UID=" + mUID + ", Name=" + mName;
	}
}
