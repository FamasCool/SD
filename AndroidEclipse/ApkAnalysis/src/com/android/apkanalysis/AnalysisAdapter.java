package com.android.apkanalysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AnalysisAdapter extends BaseAdapter {

	private Resources mResources;
	private LayoutInflater mInflater;
	private SharedPreferences mSharedPreferences;
	private SimpleDateFormat mSimpleDateFormat;
	private ArrayList<ApkAnalysisInfo> mList;
	private long mStartTime;
	private long mStopTime;

	public AnalysisAdapter(Context context, ArrayList<ApkAnalysisInfo> list) {
		mResources = context.getResources();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		mList = list;
		mStartTime = mSharedPreferences.getLong(Utils.START_TIME_KEY, -1);
		mStopTime = mSharedPreferences.getLong(Utils.STOP_TIME_KEY, -1);
	}

	public void setList(ArrayList<ApkAnalysisInfo> list) {
		mList = list;
		mStartTime = mSharedPreferences.getLong(Utils.START_TIME_KEY, -1);
		mStopTime = mSharedPreferences.getLong(Utils.STOP_TIME_KEY, -1);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_view, parent, false);
			holder = new ViewHolder();
			holder.mName = (TextView) convertView.findViewById(R.id.apk_name);
			holder.mCpuMax = (TextView) convertView.findViewById(R.id.max_cpu_usage);
			holder.mCpuAve = (TextView) convertView.findViewById(R.id.ave_cpu_usage);
			holder.mMemoryMax = (TextView) convertView.findViewById(R.id.max_memory_usage);
			holder.mMemoryAve = (TextView) convertView.findViewById(R.id.ave_memory_usage);
			holder.mTime = (TextView) convertView.findViewById(R.id.time);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		ApkAnalysisInfo info = mList.get(position);
		holder.mName.setText(info.getName());
		holder.mCpuMax.setText(info.getMaxCpuUsage() + "%");
		holder.mCpuAve
				.setText(mResources.getString(R.string.cpu_display, info.getAveCpuUsage() + "%", info.getCpuTimes()));
		holder.mMemoryMax.setText(Utils.formatStorage(info.getMaxMemoryUsage() * 1024));
		holder.mMemoryAve.setText(mResources.getString(R.string.memory_display,
				Utils.formatStorage(info.getAveMemoryUsage() * 1024), info.getMemoryTimes()));
		if (mStartTime == -1) {
			holder.mTime.setText(Utils.formatDate(0));
		} else if (mStartTime > 0) {
			if (mStopTime == -1) {
				holder.mTime.setText(Utils.formatDate(Calendar.getInstance().getTimeInMillis() - mStartTime));
			} else {
				holder.mTime.setText(Utils.formatDate(mStopTime - mStartTime));
			}
		}
		return convertView;
	}

	class ViewHolder {
		public TextView mName;
		public TextView mCpuMax;
		public TextView mCpuAve;
		public TextView mMemoryMax;
		public TextView mMemoryAve;
		public TextView mTime;
	}

}
