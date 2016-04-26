package com.android.apkanalysis;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectApkActivity extends ListActivity implements OnItemClickListener {

	private ApkListAdapter mAdapter;
	private ArrayList<ApkInfo> mList;
	private AnalysisDatabaseHelper mDatabaseHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDatabaseHelper = new AnalysisDatabaseHelper(this);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		getListView().setItemsCanFocus(false);
		getListView().setOnItemClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mList = getApkInfoList();
		mAdapter = new ApkListAdapter(this, mList);
		setListAdapter(mAdapter);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finishActivity();
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finishActivity();
	}

	private ArrayList<String> getDataBaseApkPackageName() {
		ArrayList<String> result = new ArrayList<String>();
		Cursor c = mDatabaseHelper.queryAllApkPackageName();
		if (c != null) {
			if (c.getCount() > 0) {
				while (c.moveToNext()) {
					String packageName = c.getString(c.getColumnIndexOrThrow(AnalysisDatabaseHelper.PACKAGE_NAME));
					result.add(packageName);
				}
			}
			c.close();
		}
		Log.d(this, "getDataBaseApkPackageName=>size: " + result.size());
		return result;
	}

	private ArrayList<ApkInfo> getApkInfoList() {
		ArrayList<ApkInfo> result = new ArrayList<ApkInfo>();
		ArrayList<String> lastApks = getDataBaseApkPackageName();
		PackageManager pm = getPackageManager();
		List<PackageInfo> list = pm.getInstalledPackages(0);
		if (list != null) {
			PackageInfo info = null;
			Drawable icon = null;
			CharSequence name = null;
			String packageName = null;
			for (int i = 0; i < list.size(); i++) {
				info = list.get(i);
				icon = info.applicationInfo.loadIcon(pm);
				if (icon == null) {
					icon = getResources().getDrawable(R.drawable.ic_launcher);
				}
				name = info.applicationInfo.loadLabel(pm);
				if (null == name) {
					name = getResources().getString(R.string.unknow_apk);
				}
				packageName = info.packageName;
				result.add(new ApkInfo(icon, name.toString(), packageName, null, lastApks.contains(packageName)));
			}
		}
		Log.d(this, "getApkInfoList=>size: " + result.size());
		return result;
	}

	private void finishActivity() {
		mDatabaseHelper.clearApkRecorder();
		ArrayList<Integer> checkedIds = mAdapter.getCheckedItemIds();
		Log.d(this, "finishActivity=>checked size: " + checkedIds.size());
		for (int i = 0; i < checkedIds.size(); i++) {
			Log.d(this, "finishActivity=>info: " + mList.get(checkedIds.get(i)));
			mDatabaseHelper.insertApk(mList.get(checkedIds.get(i)));
		}
		setResult(RESULT_OK, getIntent().putExtra(Utils.EXTRA_NEED_RELOAD, true));
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ApkInfo info = (ApkInfo) mAdapter.getItem(position);
		if (info.isChecked()) {
			info.setChecked(false);
		} else {
			info.setChecked(true);
		}
		mAdapter.notifyDataSetChanged();
	}

}
