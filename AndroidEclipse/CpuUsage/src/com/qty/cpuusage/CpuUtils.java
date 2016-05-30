package com.qty.cpuusage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

public class CpuUtils {

	private static final boolean DEBUG = true;
	private static final String TAG = "CpuUtils";

	private static final String CPU_CORE_PATH = "/sys/devices/system/cpu/";
	private static final String CPU_MAX_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
	private static final String CPU_MIN_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/getMinCpuFreq";
	private static final String CURRENT_CPU_FREQ_PATH = "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_cur_freq";

	public static String getCpuType() {
		String type = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(CPU_MAX_FREQ_PATH));
			String str = reader.readLine();
			while (str != null) {
				if (DEBUG) Log.d(TAG, "getCpuType=>str: " + str);
				if (str.startsWith("Hardware")) {
					int index = str.indexOf(":");
					if (index != -1) {
						type = str.substring(index + 1, str.length()).trim();
					}
					break;
				} else {
					str = reader.readLine();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getCpuType=>error: ", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (Exception e) {}
		}
		if (DEBUG)
			Log.d(TAG, "getCpuType=>type: " + type);
		return type;
	}

	public static int getCpuCore() {
		int core = -1;
		try {
			File dir = new File(CPU_CORE_PATH);
			if (dir != null && dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles(new CpuFilter());
				core = files.length;
			}
		} catch (Exception e) {
			Log.e(TAG, "getCpuCore=>error: ", e);
		}
		if (DEBUG)
			Log.d(TAG, "getCpuCore=>core: " + core);
		return core;
	}

	public static long getMaxCpuFreq() {
		long maxFreq = -1;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(CPU_MAX_FREQ_PATH));
			String str = reader.readLine();
			if (DEBUG) Log.d(TAG, "getMaxCpuFreq=>str: " + str);
			if (!TextUtils.isEmpty(str)) {
				maxFreq = Long.parseLong(str.trim());
			}
		} catch (Exception e) {
			Log.e(TAG, "getMaxCpuFreq=>error: ", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (Exception e) {}
		}
		if (DEBUG)
			Log.d(TAG, "getMaxCpuFreq=>maxFreq: " + maxFreq);
		return maxFreq;
	}

	public static long getMinCpuFreq() {
		long minFreq = -1;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(CPU_MIN_FREQ_PATH));
			String str = reader.readLine();
			if (DEBUG) Log.d(TAG, "getMinCpuFreq=>str: " + str);
			if (!TextUtils.isEmpty(str)) {
				minFreq = Long.parseLong(str.trim());
			}
		} catch (Exception e) {
			Log.e(TAG, "getMinCpuFreq=>error: ", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (Exception e) {}
		}
		if (DEBUG)
			Log.d(TAG, "getMinCpuFreq=>minFreq: " + minFreq);
		return minFreq;
	}

	public static long getCpuCurrentFreq(int core) {
		long freq = -1;
		BufferedReader reader = null;
		String currentFreqPath = String.format(CURRENT_CPU_FREQ_PATH, core);
		try {
			reader = new BufferedReader(new FileReader(currentFreqPath));
			String str = reader.readLine();
			if (DEBUG) Log.d(TAG, "getCpuCurrentFreq=>str: " + str);
			if (!TextUtils.isEmpty(str)) {
				freq = Long.parseLong(str.trim());
			}
		} catch (Exception e) {
			Log.e(TAG, "getCpuCurrentFreq=>error: ", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (Exception e) {}
		}
		if (DEBUG)
			Log.d(TAG, "getCpuCurrentFreq=>freq: " + freq + ", core: " + core);
		return freq;
	}

	public static String formatFrequency(long freq) {
		String result = null;
		DecimalFormat df = new DecimalFormat("#.0");
		if (freq > 0) {
			if (freq < 1000) {
				result = freq + "KHz";
			} else if (freq >= 1000 && freq < 1000000) {
				result = df.format((double) freq / 1000) + "MHz";
			} else if (freq >= 1000000) {
				result = df.format((double) freq / 1000000) + "GHz";
			}
		}
		if (DEBUG)
			Log.d(TAG, "formatFrequency=>result: " + result + ", freq: " + freq);
		return result;
	}

	private static class CpuFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			if (Pattern.matches("cpu[0-9]", pathname.getName())) {
				if (pathname.isDirectory()) {
					return true;
				}
			}
			return false;
		}
	}

}
