package com.android.apkanalysis;

import com.android.apkanalysis.IServiceCallback;

interface IApkAnalysisService {

	void startRecord();
	void stopRecord();
	void refreshRecordApk();
	void refreshTimeChanged(int time);
	boolean isRecording();
	void registerCallback(in IServiceCallback callback);
	void unregisterCallback(in IServiceCallback callback);
	
}
