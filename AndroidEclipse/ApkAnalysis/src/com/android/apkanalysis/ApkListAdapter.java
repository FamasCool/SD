package com.android.apkanalysis;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ApkListAdapter extends BaseAdapter {
	
	private LayoutInflater mInflater;
	private ArrayList<ApkInfo> mList;
	
	public ApkListAdapter(Context context, ArrayList<ApkInfo> list) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mList = list;
	}
	
	public ArrayList<Integer> getCheckedItemIds() {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ApkInfo info = null;
		for (int i = 0; i < mList.size(); i++) {
			info = mList.get(i);
			if (info.isChecked()) {
				ids.add(i);
			}
		}
		return ids;
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
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.apk_list_item_view, parent, false);
			holder = new ViewHolder();
			holder.mSelectCb = (CheckBox) convertView.findViewById(R.id.item_checked);
			holder.mIconIv = (ImageView) convertView.findViewById(R.id.apk_icon);
			holder.mNameTv = (TextView) convertView.findViewById(R.id.apk_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		ApkInfo info = mList.get(position);
		holder.mSelectCb.setChecked(info.isChecked());
		holder.mIconIv.setImageDrawable(info.getIcon());
		holder.mNameTv.setText(info.getLabel());
		return convertView;
	}
	
	class ViewHolder {
		public CheckBox mSelectCb;
		public ImageView mIconIv;
		public TextView mNameTv;
	}

}
