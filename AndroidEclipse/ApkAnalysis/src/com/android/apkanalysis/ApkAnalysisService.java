package com.android.apkanalysis;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class ApkAnalysisService extends Service {

	private static final int MSG_RECORD = 0;

	public static final int STATE_OFF = 0;
	public static final int STATE_ON = 1;

	private AnalysisAsyncTask mAnalysisTask;
	private ReloadProcessNameTask mReloadTask;
	private ActivityManager mActivityManager;
	private PackageManager mPackageManager;
	private RemoteCallbackList<IServiceCallback> mCallbacks;
	private AnalysisDatabaseHelper mDataHelper;
	private ArrayList<ApkProcessInfo> mApkProcessInfos;

	private int mDelayedTime;
	private boolean mIsRecording;
	private boolean mIsReady;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		mCallbacks = new RemoteCallbackList<IServiceCallback>();
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		mPackageManager = getPackageManager();
		mDelayedTime = PreferenceManager.getDefaultSharedPreferences(this).getInt(Utils.REFRESH_TIME_KEY,
				getResources().getInteger(R.integer.refresh_time));
		mDataHelper = new AnalysisDatabaseHelper(this);
		mReloadTask = new ReloadProcessNameTask();
		mReloadTask.execute(new Void[] {});
		mIsRecording = false;
		mIsReady = false;
		startForegroundService();
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(this, "onStartCommand=>intent: " + intent + " flags: " + flags + " startId: " + startId);
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		if (mHandler.hasMessages(MSG_RECORD)) {
			mHandler.removeMessages(MSG_RECORD);
		}
		if (mIsRecording) {
			Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
			e.putLong(Utils.STOP_TIME_KEY, Calendar.getInstance().getTimeInMillis());
			e.commit();
		}
		mIsRecording = false;
		stopAnaylsisTask();
		stopReloatTask();
		super.onDestroy();
	}

	private String getProcessInfo(String cmd) {
		String str = null;
		try {
			int result = ShellExe.execCommand(cmd);
			if (result == ShellExe.RESULT_SUCCESS) {
				str = ShellExe.getOutput();
			} else if (result == ShellExe.RESULT_FAIL) {
				Log.e(this, "getProcessInfo=>result fail");
			} else if (result == ShellExe.RESULT_EXCEPTION) {
				Log.e(this, "getProcessinfo=>result exception");
			}
		} catch (Exception e) {
			Log.e(this, "getProcessInfo=>error: ", e);
		}
		return str;
	}

	private long getMemoryUseSize(int pid) {
		long result = 0;
		if (pid >= 0) {
			try {
				android.os.Debug.MemoryInfo[] infos = mActivityManager.getProcessMemoryInfo(new int[] { pid });
				if (infos != null && infos.length >= 1) {
					result = infos[0].getTotalPss();
				}
			} catch (Exception e) {
				Log.e(this, "getMemoryUseInfo=>error: ", e);
			}
		}
		return result;
	}
	
	private ArrayList<ApkProcessInfo> reloadRecordApkProcess() {
		ArrayList<ApkProcessInfo> result = new ArrayList<ApkProcessInfo>();
		ArrayList<ApkInfo> apkInfos = getDataBaseApkInfos();

		ApkInfo ai = null;
		for (int i = 0; i < apkInfos.size(); i++) {
			ai = apkInfos.get(i);
			ApkProcessInfo api = new ApkProcessInfo(ai.getPackageName(), ai.getPackageName());
			api.setProcessList(getApkProcesses(ai.getPackageName()));
			Log.d(this, "reloadRecordApkProcess=>api: " + api.toString());
			result.add(api);
		}
		return result;
	}

	private ArrayList<ApkInfo> getDataBaseApkInfos() {
		ArrayList<ApkInfo> result = new ArrayList<ApkInfo>();
		Cursor c = mDataHelper.queryAllApk();

		if (c != null) {
			String name = null;
			String packageName = null;
			if (c.getCount() > 0) {
				while (c.moveToNext()) {
					name = c.getString(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.APK_NAME));
					packageName = c.getString(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.PACKAGE_NAME));
					result.add(new ApkInfo(null, name, packageName, null, false));
				}
			}
			c.close();
		}
		return result;
	}

	private ArrayList<String> getApkProcesses(String packageName) {
		ArrayList<String> result = new ArrayList<String>();
		result.add(packageName);
		String process = null;
		try {
			PackageInfo pi = mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES
					| PackageManager.GET_PROVIDERS | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);
			process = pi.applicationInfo.processName;
			if (!isContainer(result, process)) {
				result.add(process);
			}
			ActivityInfo[] ais = pi.activities;
			ActivityInfo ai = null;
			if (ais != null && ais.length > 0) {
				for (int i = 0; i < ais.length; i++) {
					ai = ais[i];
					if (!isContainer(result, ai.processName)) {
						result.add(ai.processName);
					}
				}
			}
			ServiceInfo[] sis = pi.services;
			ServiceInfo si = null;
			if (sis != null && sis.length > 0) {
				for (int i = 0; i < sis.length; i++) {
					si = sis[i];
					if (!isContainer(result, si.processName)) {
						result.add(si.processName);
					}
				}
			}
			ProviderInfo[] pdis = pi.providers;
			ProviderInfo pdi = null;
			if (pdis != null && pdis.length > 0) {
				for (int i = 0; i < pdis.length; i++) {
					pdi = pdis[i];
					if (!isContainer(result, pdi.processName)) {
						result.add(pdi.processName);
					}
				}
			}
			ais = null;
			ais = pi.receivers;
			ai = null;
			if (ais != null && ais.length > 0) {
				for (int i = 0; i < ais.length; i++) {
					ai = ais[i];
					if (!isContainer(result, ai.processName)) {
						result.add(ai.processName);
					}
				}
			}
		} catch (NameNotFoundException e) {
			Log.e(this, "getApkProcesses=>size: " + result.size() + " packageName: " + packageName);
		}
		return result;
	}
	
	private boolean isContainer(ArrayList<String> list, String value) {
		boolean result = false;
		if (TextUtils.isEmpty(value)) {
			result = true;
		} else {
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i).equals(value)) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	private void notifyRecordStateChanged() {
		int length = mCallbacks.beginBroadcast();
		for (int i = 0; i < length; i++) {
			IServiceCallback callback = mCallbacks.getBroadcastItem(i);
			try {
				callback.onRecordStateChanged(mIsRecording);
			} catch (RemoteException e) {
				Log.e(this, "notifyRecordStateChanged=>error: ", e);
			}
		}
		mCallbacks.finishBroadcast();
	}

	private void notifyRecordDataChanged() {
		int length = mCallbacks.beginBroadcast();
		for (int i = 0; i < length; i++) {
			IServiceCallback callback = mCallbacks.getBroadcastItem(i);
			try {
				callback.onDataChanged();
			} catch (RemoteException e) {
				Log.e(this, "notifyRecordStateChanged=>error: ", e);
			}
		}
		mCallbacks.finishBroadcast();
	}

	private ArrayList<String> getTopProcessInfos(String top) {
		String[] strs = top.split(" ");
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < strs.length; i++) {
			if (!"".equals(strs[i]) && !" ".equals(strs[i])) {
				list.add(strs[i]);
			}
		}
		if (list.size() > 10) {
			String end = list.get(9);
			int index = top.lastIndexOf(end);
			if (index > 0) {
				String name = top.substring(index, top.length());
				for (int i = 9; i < list.size(); i++) {
					list.remove(i);
				}
				list.set(9, name);
			}
		}
		return list;
	}

	private void stopAnaylsisTask() {
		if (mAnalysisTask != null && mAnalysisTask.getStatus() == AsyncTask.Status.RUNNING) {
			mAnalysisTask.cancel(true);
		}
		mAnalysisTask = null;
	}

	private void stopReloatTask() {
		if (mReloadTask != null && mReloadTask.getStatus() == AsyncTask.Status.RUNNING) {
			mReloadTask.cancel(true);
		}
		mReloadTask = null;
	}
	
	private ArrayList<AnalysisInfo> createAnalysisInfos() {
		ArrayList<AnalysisInfo> result = new ArrayList<AnalysisInfo>();
		ArrayList<ApkInfo> ais = getDataBaseApkInfos();
		Calendar c = Calendar.getInstance();
		ApkInfo ai = null;
		for (int i = 0; i < ais.size(); i++) {
			ai = ais.get(i);
			AnalysisInfo info = new AnalysisInfo(ai.getPackageName(), c.getTimeInMillis());
			result.add(info);
		}
		return result;
	}

	private void startForegroundService() {
		Notification status = new Notification(0, null, System.currentTimeMillis());
		// status.flags |= Notification.FLAG_HIDE_NOTIFICATION;
		status.flags |= 0x10000000;
		startForeground(10, status);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(this, "handleMessage=>what: " + msg.what);
			switch (msg.what) {
			case MSG_RECORD:
				if (mIsReady) {
					if (mAnalysisTask != null && (mAnalysisTask.getStatus() != AsyncTask.Status.FINISHED)) {
						mAnalysisTask.cancel(true);
						mAnalysisTask = null;
					}
					mAnalysisTask = new AnalysisAsyncTask();
					mAnalysisTask.execute(new Void[] {});
					mHandler.sendEmptyMessageDelayed(MSG_RECORD, mDelayedTime * 1000);
				}
				break;
			}
		}
	};

	class ReloadProcessNameTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mApkProcessInfos = reloadRecordApkProcess();
			mDataHelper.clearProcessRecorder();
			Log.d(this, "doInBackground=>size: " + mApkProcessInfos.size());
			for (int i = 0; i < mApkProcessInfos.size(); i++) {
				mDataHelper.insertProcess(mApkProcessInfos.get(i));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(this, "onPostExecute=>reload process name finish.");
			mIsReady = true;
			if (mIsRecording) {
				if (mHandler.hasMessages(MSG_RECORD)) {
					mHandler.removeMessages(MSG_RECORD);
				}
				stopAnaylsisTask();
				mDataHelper.clearAnalysisRecorder();
				mHandler.sendEmptyMessage(MSG_RECORD);
			}
		}

	};

	class AnalysisAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			ArrayList<AnalysisInfo> infos = createAnalysisInfos();
			String result = getProcessInfo("top -n 1");
			String[] strs = result.split("\n");
			ArrayList<String> tpis = null;
			ApkProcessInfo api = null;
			AnalysisInfo ai = null;
			int pid = -1;
			int cpu = 0;
			long pss = 0;
			Log.d(this, "doInBackground=>length: " + strs.length);
			for (int i = 0; i < strs.length; i++) {
				if (i >= 7 && !TextUtils.isEmpty(strs[i])) {
					tpis = getTopProcessInfos(strs[i]);
					for (int j = 0; j < mApkProcessInfos.size(); j++) {
						api = mApkProcessInfos.get(j);
						if (api.isApkProcess(tpis.get(9))) {
							pid = Utils.parseInt(tpis.get(0));
							cpu = Utils.parseCpu(tpis.get(2));
							pss = getMemoryUseSize(pid);
							Log.d(this, "doInBackground=>process: " + tpis.get(9) + " pid: " + pid + " cpu: " + cpu
									+ " pss: " + pss);
							infos.get(j).increaseCpu(cpu);
							infos.get(j).increaseMemory(pss);
							break;
						}
					}
				}
				tpis = null;
				api = null;
				ai = null;
				pid = -1;
				cpu = 0;
				pss = 0;
			}
			for (int i = 0; i < infos.size(); i++) {
				mDataHelper.insertAnalysis(infos.get(i));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			Log.d(this, "onPostExecute=>analysis finish.");
			notifyRecordDataChanged();
			super.onPostExecute(param);
		}

	}

	private IApkAnalysisService.Stub mBinder = new IApkAnalysisService.Stub() {

		@Override
		public void stopRecord() throws RemoteException {
			Log.d(this, "stopRecord=>isRecording: " + mIsRecording);
			if (mIsRecording) {
				mHandler.removeMessages(MSG_RECORD);
				mIsRecording = false;
				notifyRecordStateChanged();
			}
		}

		@Override
		public void startRecord() throws RemoteException {
			Log.d(this, "stopRecord=>startRecord: " + mIsRecording);
			if (!mIsRecording) {
				mHandler.sendEmptyMessage(MSG_RECORD);
				mIsRecording = true;
				notifyRecordStateChanged();
			}
		}

		@Override
		public void refreshRecordApk() throws RemoteException {
			Log.d(this, "refreshRecordApk()...");
			stopReloatTask();
			mReloadTask = new ReloadProcessNameTask();
			mReloadTask.execute(new Void[] {});
		}

		@Override
		public void refreshTimeChanged(int time) throws RemoteException {
			Log.d(this, "refreshTimeChanged=>time: " + time);
			mDelayedTime = time;
			if (mIsRecording) {
				stopAnaylsisTask();
				if (mHandler.hasMessages(MSG_RECORD)) {
					mHandler.removeMessages(MSG_RECORD);
				}
				mHandler.sendEmptyMessage(MSG_RECORD);
			}
		}

		@Override
		public void registerCallback(final IServiceCallback callback) throws RemoteException {
			Log.d(this, "registerCallback=>callback: " + callback);
			if (callback != null) {
				mCallbacks.register(callback);
				callback.asBinder().linkToDeath(new DeathRecipient() {

					@Override
					public void binderDied() {
						mCallbacks.unregister(callback);
					}
				}, 0);
			}
		}

		@Override
		public void unregisterCallback(final IServiceCallback callback) throws RemoteException {
			Log.d(this, "unregisterCallback=>callback: " + callback);
			if (callback != null) {
				mCallbacks.unregister(callback);
				callback.asBinder().linkToDeath(new DeathRecipient() {

					@Override
					public void binderDied() {
						mCallbacks.unregister(callback);
					}
				}, 0);
			}
		}

		@Override
		public boolean isRecording() throws RemoteException {
			Log.d(this, "isRecording=>isRecording: " + mIsRecording);
			return mIsRecording;
		}
	};

}
