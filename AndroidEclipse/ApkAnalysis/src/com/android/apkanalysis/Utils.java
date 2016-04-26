package com.android.apkanalysis;

import java.text.DecimalFormat;

public class Utils {
	
	public static final String REFRESH_TIME_KEY = "refresh_time";
	public static final String START_TIME_KEY = "start_time";
	public static final String STOP_TIME_KEY = "stop_time";
	public static final String EXTRA_NEED_RELOAD = "need_reload";
	
	public static String formatDate(long time) {
		String date = "";
		long SECONDS = 1000;
		long MINUTE = 60 * SECONDS;
		long HOUR = 60 * MINUTE;
		long DAY = 24 * HOUR;
		int day = 0;
		int hour = 0;
		int minute = 0;
		int seconds = 0;
		if (time >= 0) {
			day = (int) (time / DAY);
			hour = (int) ((time - (day * DAY)) / HOUR);
			minute = (int)((time - (day * DAY) - (hour * HOUR)) / MINUTE);
			seconds = (int)((time % MINUTE) / SECONDS);
		}
		
		if (day > 0) {
			date = day + " 天";
		}
		if (hour > 0) {
			date = date + hour + " 时";
		}
		if (minute > 0) {
			date = date + minute + " 分";
		}
		
		date = date + seconds + " 秒";
		
		return date;
	}
	
	public static String formatStorage(long size) {
		String result = "";
		long KB = 1024;
		long MB = KB * 1024;
		long GB = MB * 1024;
		DecimalFormat df = new DecimalFormat("#.00");
		if (size < 0) {
			result = "0 B";
		} else if (size >= 0 && size < KB) {
			result = size + " B";
		} else if (size >= KB && size < MB) {
			result = df.format((double) size / KB) + " KB";
		} else if (size >= MB && size < GB) {
			result = df.format((double) size / MB) + " MB";
		} else if (size >= GB) {
			result = df.format((double) size / GB) + " GB";
		}
		Log.d("Utils", "formatStorage=>result: " + result + " size: " + size);
		return result;
	}
	
	public static int parseInt(String str) {
		int result = -1;
		try {
			result = Integer.parseInt(str);
		} catch (Exception e) {
			Log.e("Utils", "parseInt=>error: ", e);
		}
		return result;
	}
	
	public static int parseCpu(String cpu) {
		int result = 0;
		if (cpu.length() >= 2) {
			try {
				result = Integer.parseInt(cpu.substring(0, cpu.length() - 1));
			} catch (Exception e) {
				Log.e("Utils", "parseCpu=>error: ", e);
			}
		}
		return result;
	}

}
