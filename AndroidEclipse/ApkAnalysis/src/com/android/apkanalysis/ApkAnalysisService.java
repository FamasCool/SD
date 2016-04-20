package com.android.apkanalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

public class ApkAnalysisService extends Service {

	private static final int MSG_RECORD = 0;

	private static final int RECORD_DELAYED = 30 * 1000;

	private TopAsyncTask mTask;
	private ActivityManager mActivityManager;

	private String mRecordFilePath;
	private int mCurrentHour;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		mRecordFilePath = getCurrentRecordFilePath();
		registerReceiver(mTimeChangedReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		mHandler.sendEmptyMessageDelayed(MSG_RECORD, RECORD_DELAYED);
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
		unregisterReceiver(mTimeChangedReceiver);
		if (mHandler.hasMessages(MSG_RECORD)) {
			mHandler.removeMessages(MSG_RECORD);
		}
		if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
			mTask.cancel(true);
		}
		super.onDestroy();
	}

	private void startForegroundService() {
		Notification status = new Notification(0, null, System.currentTimeMillis());
		// status.flags |= Notification.FLAG_HIDE_NOTIFICATION;
		status.flags |= 0x10000000;
		startForeground(10, status);
	}

	private String getCurrentRecordFilePath() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
		Calendar c = Calendar.getInstance();
		mCurrentHour = c.get(Calendar.HOUR_OF_DAY);
		String currentTime = sdf.format(c.getTime());
		String recordFileName = currentTime + ".txt";
		File recordFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), recordFileName);
		if (!recordFile.exists()) {
			try {
				recordFile.createNewFile();
			} catch (IOException e) {
				Log.e(this, "getCurrentRecordFilePath=>error: ", e);
			}
		}
		Log.d(this, "getCureentRecordFilePath=>path: " + recordFile.getAbsolutePath());
		return recordFile.getAbsolutePath();
	}

	private void updateCurrentRecordFilePath() {
		Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		Log.d(this, "updateCurrentRecordFilePath=>hour: " + hour + " last: " + mCurrentHour);
		if (hour != mCurrentHour) {
			mRecordFilePath = getCurrentRecordFilePath();
		}
	}

	private String getProcessInfo(String cmd) {
		String str = null;
		try {
			int result = ShellExe.execCommand("top -n 1");
			if (result == ShellExe.RESULT_SUCCESS) {
				str = ShellExe.getOutput();
			} else if (result == ShellExe.RESULT_FAIL) {
				/// TODO: do fail in this
			} else if (result == ShellExe.RESULT_EXCEPTION) {
				/// TODO: do exception in this
			}
		} catch (Exception e) {
			Log.e(this, "getProcessInfo=>error: ", e);
		}
		return str;
	}

	private ArrayList<ApkInfo> getThirdApkInfoList() {
		ArrayList<ApkInfo> result = new ArrayList<ApkInfo>();
		PackageManager pm = getPackageManager();
		List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
		PackageInfo info = null;
		int flag = 0;
		String label = null;
		String packageName = null;
		String processName = null;
		if (packageInfoList != null) {
			for (int i = 0; i < packageInfoList.size(); i++) {
				info = packageInfoList.get(i);
				if (info != null) {
					flag = info.applicationInfo.flags;
					if ((flag & ApplicationInfo.FLAG_SYSTEM) == 0) {
						label = (String) info.applicationInfo.loadLabel(pm);
						packageName = info.applicationInfo.packageName;
						processName = info.applicationInfo.processName;
						result.add(new ApkInfo(label, packageName, processName));
						//Log.d(this, "getThirdApkInfoList=>label: " + label + ", packageName: " + packageName
						//		+ ", processName: " + processName);
					}
				}
			}
		}
		Log.d(this, "getThirdApkInfoList=>size: " + result.size());
		return result;
	}

	private ProcessInfo createProcessInfo(String str) {
		ProcessInfo info = null;
		String[] strs = str.split(" ");
		ArrayList<String> infoList = new ArrayList<String>();
		for (int i = 0; i < strs.length; i++) {
			if (!TextUtils.isEmpty(strs[i])) {
				infoList.add(strs[i]);
			}
		}
		//Log.d(this, "createProcessInfo=>size: " + infoList.size());
		if (infoList.size() == 10) {
			info = new ProcessInfo(infoList);
		}
		//Log.d(this, "createProcessInfo=>info: " + (info != null ? info.toString() : "null") + " str: " + str);
		return info;
	}

	private StringBuilder getRecordMessage(ArrayList<ApkInfo> apkInfo, ArrayList<ProcessInfo> processInfo) {
		StringBuilder result = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		String currentTime = sdf.format(c.getTime());

		result.append("\n");
		result.append(" --------------------------------------------------------\n");
		result.append("|Record Time: " + currentTime + "                        |\n");
		result.append(" --------------------------------------------------------\n");
		result.append("|Apk Name                      |Cpu use     |Memory use  |\n");
		result.append(" --------------------------------------------------------\n");
		ApkInfo apk = null;
		ProcessInfo process = null;
		boolean isActive = false;
		int cpuUse = 0;
		long memoryUse = 0;
		for (int i = 0; i < apkInfo.size(); i++) {
			apk = apkInfo.get(i);
			// Log.d(this, "getRecordMessage=>apk: " + apk.toString());
			for (int j = 0; j < processInfo.size(); j++) {
				process = processInfo.get(j);
				// Log.d(this, "getRecordMessage=>process: " +
				// process.toString());
				if (process.getName().contains(apk.getProcessName())) {
					cpuUse += getCpuUse(process.getCpu());
					memoryUse += getMemoryUseSize(process.getPid());
					Log.d(this, "getRecordMessage=>apk: " + apk.toString() + " process: " + process.getName() + " cpu: "
							+ getCpuUse(process.getCpu()) + " memory: " + getMemoryUseSize(process.getPid()));
					isActive = true;
				}
			}
			// Log.d(this, "getRecordMessage=>active: " + isActive);
			if (!isActive) {
				result.append("|" + String.format("%-30s", apk.getLabel()) + "|" + String.format("%-12s", "inactive")
						+ "|" + String.format("%-12s", "inactive") + "|\n");
				result.append("---------------------------------------------------------\n");
			} else {
				// result.append("|" + String.format("%-30s", apk.getLabel()) +
				// "|"
				// + String.format("%-12s", process.getCpu()) + "|"
				// + String.format("%-12s", formatMemoryUse(process.getRss())) +
				// "|\n");
				result.append(
						"|" + String.format("%-30s", apk.getLabel()) + "|" + String.format("%-12s", (cpuUse + "%"))
								+ "|" + String.format("%-12s", formatStorage(memoryUse)) + "|\n");
				result.append(" --------------------------------------------------------\n");
			}
			Log.d(this, "\n");
			cpuUse = 0;
			memoryUse = 0;
			isActive = false;
		}

		return result;
	}

	private int getCpuUse(String cpuStr) {
		int cpu = 0;
		if (!TextUtils.isEmpty(cpuStr) && cpuStr.length() >= 2) {
			try {
				String c = cpuStr.substring(0, cpuStr.length() - 1);
				cpu = Integer.parseInt(c);
			} catch (Exception e) {
				Log.e(this, "getCpuUse=>error: ", e);
			}
		}
		Log.d(this, "getCpuUse=>cpu: " + cpu + " str: " + cpuStr);
		return cpu;
	}

	private long getMemoryUseSize(String pidStr) {
		long result = 0;
		if (!TextUtils.isEmpty(pidStr)) {
			try {
				int pid = Integer.parseInt(pidStr);
				android.os.Debug.MemoryInfo[] infos = mActivityManager.getProcessMemoryInfo(new int[] { pid });
				if (infos != null && infos.length >= 1) {
					result = infos[0].getTotalPss();//infos[0].getTotalPrivateDirty() + infos[0].getTotalPrivateClean() + infos[0].getT;
					//Log.d(this, "getMemoryUseInfo=>size: " + result);
				}
			} catch (Exception e) {
				Log.e(this, "getMemoryUseInfo=>error: ", e);
			}
		}
		return result;
	}

	private String formatMemoryUse(String memoryUse) {
		String result = null;
		if (memoryUse != null && !TextUtils.isEmpty(memoryUse) && memoryUse.length() >= 2) {
			String memoryStr = memoryUse.substring(0, memoryUse.length() - 1);
			try {
				long memory = Long.parseLong(memoryStr);
				result = formatStorage(memory);
			} catch (Exception e) {
				Log.e(this, "formatMemoryUse=>error: ", e);
			}
		}
		return result;
	}

	private String formatStorage(long storage) {
		String result = null;
		long MB = 1024;
		long GB = 1024 * MB;
		DecimalFormat df = new DecimalFormat("#.00");
		if (storage >= 0 && storage < MB) {
			result = storage + "KB";
		} else if (storage >= MB && storage < GB) {
			result = df.format((double) storage / MB) + "MB";
		} else if (storage >= GB) {
			result = df.format((double) storage / GB) + "GB";
		}
		return result;
	}

	private BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action)) {
				updateCurrentRecordFilePath();
			}
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(this, "handleMessage=>what: " + msg.what);
			switch (msg.what) {
			case MSG_RECORD:
				if (mTask != null && (mTask.getStatus() != AsyncTask.Status.FINISHED)) {
					mTask.cancel(true);
					mTask = null;
				}
				mTask = new TopAsyncTask();
				mTask.execute(new Void[] {});
				mHandler.sendEmptyMessageDelayed(MSG_RECORD, RECORD_DELAYED);
				break;
			}
		}
	};

	class TopAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			ArrayList<ApkInfo> apkInfoList = getThirdApkInfoList();
			ArrayList<ProcessInfo> processInfoList = new ArrayList<ProcessInfo>();
			String result = getProcessInfo("top -n 1");
			String[] strs = result.split("\n");
			Log.d(this, "doInBackground=>length: " + strs.length);
			for (int i = 0; i < strs.length; i++) {
				// Log.d(this, "doInBackground=>line(" + i + "): " + strs[i]);
				if (i >= 7 && !TextUtils.isEmpty(strs[i])) {
					ProcessInfo info = createProcessInfo(strs[i]);
					if (info != null) {
						// Log.d(this, "doInBackground=>info: " +
						// info.toString());
						processInfoList.add(info);
					}
				}
			}
			Log.d(this, "doInBackground=>process size: " + processInfoList.size());
			StringBuilder sb = getRecordMessage(apkInfoList, processInfoList);
			File file = new File(mRecordFilePath);
			Log.d(this, "doInBackground=>exists: " + file.exists() + " canWrite: " + file.canWrite());
			try {
				if (!file.exists()) {
					file.createNewFile();
				}
				FileWriter fWriter = new FileWriter(file, true);
				BufferedWriter out = new BufferedWriter(fWriter);
				out.write(sb.toString());
				out.close();
			} catch (IOException e) {
				Log.e(this, "doInBackground=>error: ", e);
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			Log.d(this, "onCancelled()...");
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void param) {
			Log.d(this, "onPostExecute()...");
			super.onPostExecute(param);
		}

	}

}
