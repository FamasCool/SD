package com.android.apkanalysis;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class ApkAnalysisActivity extends Activity {

	private static final int REQUST_CODE = 10;

	private ListView mListView;
	private IApkAnalysisService mService;
	private ServiceToken mServiceToken;
	private MenuItem mStartMenu;
	private MenuItem mResumeMenu;
	private MenuItem mStopMenu;
	private AlertDialog mSetTimeDialog;
	private EditText mTimeEt;
	private SharedPreferences mSharedPreferences;
	private AnalysisDatabaseHelper mDatabaseHelper;
	private AnalysisAdapter mAdapter;
	private ArrayList<ApkAnalysisInfo> mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_apk_analysis);

		startRecordService();
		bindToAnalysisService();
		mListView = (ListView) findViewById(R.id.list);
		mListView.setClickable(false);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mDatabaseHelper = new AnalysisDatabaseHelper(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshListView();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unbindFromService(mServiceToken);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.apk_analysis, menu);
		mStartMenu = menu.findItem(R.id.action_start);
		mResumeMenu = menu.findItem(R.id.action_resume);
		mStopMenu = menu.findItem(R.id.action_stop);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean recording = isRecording();
		mStartMenu.setVisible(!recording);
		mResumeMenu.setVisible(!recording);
		mStopMenu.setVisible(recording);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_start:
			Log.d(this, "onMenuItemSelected=>start: " + mService);
			mDatabaseHelper.clearAnalysisRecorder();
			if (mService != null) {
				try {
					mService.startRecord();
					Editor e = mSharedPreferences.edit();
					e.putLong(Utils.START_TIME_KEY, Calendar.getInstance().getTimeInMillis());
					e.putLong(Utils.STOP_TIME_KEY, -1);
					e.commit();
				} catch (RemoteException e) {
					Log.e(this, "onMenuItemSelected=>error: ", e);
				}
			}
			break;

		case R.id.action_resume:
			Log.d(this, "onMenuItemSelected=>start: " + mService);
			if (mService != null) {
				try {
					mService.startRecord();
					Editor e = mSharedPreferences.edit();
					e.putLong(Utils.STOP_TIME_KEY, -1);
					e.commit();
				} catch (RemoteException e) {
					Log.e(this, "onMenuItemSelected=>error: ", e);
				}
			}
			break;

		case R.id.action_stop:
			Log.d(this, "onMenuItemSelected=>stop: " + mService);
			if (mService != null) {
				try {
					mService.stopRecord();
					Editor e = mSharedPreferences.edit();
					e.putLong(Utils.STOP_TIME_KEY, Calendar.getInstance().getTimeInMillis());
					e.commit();
					refreshListView();
				} catch (RemoteException e) {
					Log.e(this, "onMenuItemSelected=>error: ", e);
				}
			}
			break;

		case R.id.action_set_time:
			if (mSetTimeDialog != null) {
				if (mSetTimeDialog.isShowing()) {
					mSetTimeDialog.cancel();
				}
				mSetTimeDialog = null;
			}
			mSetTimeDialog = createSetTimeDialog();
			mSetTimeDialog.show();
			break;

		case R.id.action_select_apk:
			Intent selectApk = new Intent(this, SelectApkActivity.class);
			startActivityForResult(selectApk, REQUST_CODE);
			break;

		case R.id.action_clear_record:
			mDatabaseHelper.clearAnalysisRecorder();
			long startTime = -1;
			try {
				if (mService != null) {
					if (mService.isRecording()) {
						startTime = Calendar.getInstance().getTimeInMillis();
					}
				}
			} catch (Exception e) {
				Log.e(this, "onMenuItemSelected=>clear record error: ", e);
			}
			Editor e = mSharedPreferences.edit();
			e.putLong(Utils.START_TIME_KEY, startTime);
			e.commit();
			refreshListView();
			break;

		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(this, "onActivityResult=>requestCode: " + requestCode + " resultCode: " + resultCode + " data: " + data);
		if (requestCode == REQUST_CODE && resultCode == RESULT_OK) {
			if (data != null) {
				boolean need = data.getBooleanExtra(Utils.EXTRA_NEED_RELOAD, false);
				if (need) {
					if (mService != null) {
						try {
							mService.refreshRecordApk();
						} catch (RemoteException e) {
							Log.e(this, "onActivityResult=>error: ", e);
						}
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void refreshListView() {
		mList = getProcessInfoList();
		mAdapter = new AnalysisAdapter(this, mList);
		mListView.setAdapter(mAdapter);
	}

	private ArrayList<ApkAnalysisInfo> getProcessInfoList() {
		ArrayList<ApkAnalysisInfo> result = new ArrayList<ApkAnalysisInfo>();
		ArrayList<ApkInfo> apkInfos = getDatabaseApkInfos();

		int cpuTotalUsage = 0;
		int cpuMaxUsage = 0;
		int cpuUsage = 0;
		long memoryTotalUsage = 0;
		long memoryMaxUsage = 0;
		long memoryUsage = 0;
		long startTime = 0;
		long endTime = 0;
		int cpuTimes = 0;
		int memoryTimes = 0;
		String apkName = "";
		ApkInfo apkInfo = null;
		Cursor c = null;
		for (int i = 0; i < apkInfos.size(); i++) {
			apkInfo = apkInfos.get(i);
			c = mDatabaseHelper.queryAnalysis(apkInfo.getPackageName());
			Log.d(this, "getProcessInfoList=>info: " + apkInfo + " c: " + c);
			if (c != null) {
				if (c.getCount() > 0) {
					c.moveToFirst();
					startTime = endTime = c.getLong(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.TIME));
					cpuUsage = cpuMaxUsage = cpuTotalUsage = c
							.getInt(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.CPU));
					if (cpuUsage > 0) {
						cpuTimes++;
					}
					memoryUsage = memoryMaxUsage = memoryTotalUsage = c
							.getInt(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.PSS));
					if (memoryUsage > 0) {
						memoryTimes++;
					}
					while (c.moveToNext()) {
						cpuUsage = c.getInt(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.CPU));
						memoryUsage = c.getInt(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.PSS));
						cpuTotalUsage += cpuUsage;
						memoryTotalUsage += memoryUsage;
						if (cpuUsage > cpuMaxUsage) {
							cpuMaxUsage = cpuUsage;
						}
						if (memoryUsage > memoryMaxUsage) {
							memoryMaxUsage = memoryUsage;
						}
						if (cpuUsage > 0) {
							cpuTimes++;
						}
						if (memoryUsage > 0) {
							memoryTimes++;
						}
						endTime = c.getLong(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.TIME));
					}
					Log.d(this, "getProcessInfoList=>cpu: " + cpuTotalUsage + " cTime: " + cpuTimes + " memory: "
							+ memoryTotalUsage + " mTimes: " + memoryTimes);
					ApkAnalysisInfo info = new ApkAnalysisInfo(apkInfo.getLabel(), cpuMaxUsage,
							(cpuTimes == 0 ? 0 : cpuTotalUsage / cpuTimes), memoryMaxUsage,
							(memoryTimes == 0 ? 0 : memoryTotalUsage / memoryTimes), cpuTimes, memoryTimes,
							endTime - startTime);
					result.add(info);
				} else {
					ApkAnalysisInfo info = new ApkAnalysisInfo(apkInfo.getLabel(), 0, 0, 0, 0, 0, 0, 0);
					result.add(info);
				}
				c.close();
			}
			cpuTotalUsage = 0;
			cpuMaxUsage = 0;
			cpuUsage = 0;
			memoryTotalUsage = 0;
			memoryMaxUsage = 0;
			memoryUsage = 0;
			startTime = 0;
			endTime = 0;
			cpuTimes = 0;
			memoryTimes = 0;
			apkName = "";
		}
		Log.d(this, "getProcessInfoList=>size: " + result.size());
		return result;
	}

	private ArrayList<ApkInfo> getDatabaseApkInfos() {
		ArrayList<ApkInfo> result = new ArrayList<ApkInfo>();
		Cursor c = mDatabaseHelper.queryAllApk();
		if (c != null) {
			if (c.getCount() > 0) {
				String name = null;
				String packageName = null;
				while (c.moveToNext()) {
					name = c.getString(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.APK_NAME));
					packageName = c.getString(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.PACKAGE_NAME));
					result.add(new ApkInfo(null, name, packageName, null, false));
				}
			}
			c.close();
		}
		Log.d(this, "getDatabaseApkInfos=>size: " + result.size());
		return result;
	}

	private void startRecordService() {
		Intent service = new Intent(this, ApkAnalysisService.class);
		startService(service);
	}

	private boolean isRecording() {
		boolean result = false;
		try {
			if (mService != null) {
				result = mService.isRecording();
			}
		} catch (RemoteException e) {
			Log.e(this, "isRecording=>error: ", e);
		}
		return result;
	}

	private AlertDialog createSetTimeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.SetTimeDialogThem);
		builder.setTitle(R.string.set_time_dialog_title);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_set_time_view, null);
		mTimeEt = (EditText) view.findViewById(R.id.refresh_time);
		int time = mSharedPreferences.getInt(Utils.REFRESH_TIME_KEY, getResources().getInteger(R.integer.refresh_time));
		mTimeEt.setText(time + "");
		mTimeEt.setSelection((time + "").length());
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String timeStr = mTimeEt.getText().toString();
				if (TextUtils.isDigitsOnly(timeStr)) {
					try {
						int lastTime = mSharedPreferences.getInt(Utils.REFRESH_TIME_KEY,
								getResources().getInteger(R.integer.refresh_time));
						int time = Integer.parseInt(timeStr);
						if (time != lastTime) {
							Editor e = mSharedPreferences.edit();
							e.putInt(Utils.REFRESH_TIME_KEY, time);
							e.commit();
							if (mService != null) {
								mService.refreshTimeChanged(time);
							}
						}
					} catch (Exception e) {
						Log.e(this, "onClick=>error: ", e);
					}
				}
				dialog.dismiss();
			}
		});
		return builder.create();
	}

	private ServiceToken bindToAnalysisService() {
		Log.d(this, "bindToService()...");
		ContextWrapper cw = new ContextWrapper(this);
		cw.startService(new Intent(cw, ApkAnalysisService.class));
		if (cw.bindService((new Intent()).setClass(cw, ApkAnalysisService.class), mConnection, 0)) {
			return new ServiceToken(cw);
		}
		return null;
	}

	private void unbindFromService(ServiceToken token) {
		if (token == null) {
			return;
		}
		ContextWrapper cw = token.mWrappedContext;
		cw.unbindService(mConnection);
		mServiceToken = null;
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(this, "onServiceDisconnected()...");
			try {
				mService.unregisterCallback(mCallback);
			} catch (RemoteException e) {
				Log.e(this, "onServiceDisconnected=>error: ", e);
			}
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(this, "onServiceConnected()...");
			mService = IApkAnalysisService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
				if (!mService.isRecording()) {
					long start = mSharedPreferences.getLong(Utils.START_TIME_KEY, -1);
					long stop = mSharedPreferences.getLong(Utils.STOP_TIME_KEY, -1);
					if (start != -1 && stop == -1) {
						Editor e = mSharedPreferences.edit();
						e.putLong(Utils.STOP_TIME_KEY, Calendar.getInstance().getTimeInMillis());
						e.commit();
					}
				}
			} catch (RemoteException e) {
				Log.e(this, "onServiceConnected=>error: ", e);
			}
		}
	};

	private IServiceCallback.Stub mCallback = new IServiceCallback.Stub() {

		@Override
		public void onDataChanged() throws RemoteException {
			Log.d(this, "onDataChanged()...");
			refreshListView();
		}

		@Override
		public void onRecordStateChanged(boolean isRecording) throws RemoteException {
			Log.d(this, "onRecordStateChanged=>isRecording: " + isRecording);
		}

	};

	public static class ServiceToken {
		ContextWrapper mWrappedContext;

		ServiceToken(ContextWrapper context) {
			mWrappedContext = context;
		}
	}

}
