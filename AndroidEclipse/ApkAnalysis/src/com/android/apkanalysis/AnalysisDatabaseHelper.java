package com.android.apkanalysis;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

public class AnalysisDatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "analysis.db";
	public static final int DATABASE_VERSION = 1;

	public static final String APK_TABLE = "apk";
	public static final String PROCESS_TABLE = "process";
	public static final String ANALYSIS_TABLE = "analysis";

	public static final String ID = "id";
	public static final String APK_NAME = "apk_name";
	public static final String PACKAGE_NAME = "package_name";
	public static final String CPU = "cpu";
	public static final String PSS = "pss";
	public static final String PROCESS_NAME = "process_name";
	public static final String TIME = "time";

	public static final String[] APK_COLUMNS = { ID, APK_NAME, PACKAGE_NAME };
	public static final String[] PROCESS_COLUMNS = { ID, PACKAGE_NAME, PROCESS_NAME };
	public static final String[] ANALYSIS_COLUMNS = { ID, PACKAGE_NAME, CPU, PSS, TIME };

	public AnalysisDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + APK_TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + APK_NAME + " TEXT,"
				+ PACKAGE_NAME + " TEXT);");
		db.execSQL("CREATE TABLE " + PROCESS_TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + PACKAGE_NAME
				+ " TEXT," + PROCESS_NAME + " TEXT);");
		db.execSQL("CREATE TABLE " + ANALYSIS_TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + PACKAGE_NAME
				+ " TEXT," + CPU + " INTEGER," + PSS + " INTEGER," + TIME + " INTEGER);");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + APK_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + ANALYSIS_TABLE);
		onCreate(db);
	}

	public Cursor queryAllAnalysis() {
		Cursor result = null;
		SQLiteDatabase db = getReadableDatabase();
		result = db.query(ANALYSIS_TABLE, ANALYSIS_COLUMNS, null, null, null, null, TIME + " asc");
		return result;
	}

	public Cursor queryAnalysis(String packageName) {
		Cursor result = null;
		if (!TextUtils.isEmpty(packageName)) {
			SQLiteDatabase db = getReadableDatabase();
			result = db.query(ANALYSIS_TABLE, ANALYSIS_COLUMNS, PACKAGE_NAME + "= ? ", new String[] { packageName },
					null, null, TIME + " asc");
		}
		return result;
	}

	public long insertAnalysis(AnalysisInfo info) {
		long result = -1;
		if (info != null) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(PACKAGE_NAME, info.getPackageName());
			values.put(CPU, info.getCpu());
			values.put(PSS, info.getPss());
			values.put(TIME, info.getTime());
			result = db.insert(ANALYSIS_TABLE, null, values);
		}

		return result;
	}

	private void revertSeq(String table) {
		String sql = "update sqlite_sequence set seq=0 where name='" + table + "'";
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL(sql);
	}

	public void clearAnalysisRecorder() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + ANALYSIS_TABLE + ";");
		revertSeq(ANALYSIS_TABLE);
	}

	public Cursor queryAllApk() {
		Cursor result = null;
		SQLiteDatabase db = getReadableDatabase();
		result = db.query(APK_TABLE, APK_COLUMNS, null, null, null, null, null);
		return result;
	}

	public Cursor queryAllApkPackageName() {
		Cursor result = null;
		SQLiteDatabase db = getReadableDatabase();
		result = db.query(APK_TABLE, new String[] { PACKAGE_NAME }, null, null, null, null, null);
		return result;
	}

	public long insertApk(ApkInfo info) {
		long result = -1;
		if (info != null) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(APK_NAME, info.getLabel());
			values.put(PACKAGE_NAME, info.getPackageName());
			result = db.insert(APK_TABLE, null, values);
		}
		Log.d(this, "insertApk=>result: " + result + " info: " + info);
		return result;
	}

	public long deleteApk(String packageName) {
		long result = -1;
		if (!TextUtils.isEmpty(packageName)) {
			SQLiteDatabase db = getWritableDatabase();
			Cursor delete = db.query(APK_TABLE, new String[] { PACKAGE_NAME }, PACKAGE_NAME + "= ? ",
					new String[] { packageName }, null, null, null);
			if (delete != null) {
				delete.moveToFirst();
				int id = delete.getInt(delete.getColumnIndexOrThrow(ID));
				result = db.delete(APK_TABLE, ID + "= ? ", new String[] { id + "" });
				delete.close();
			}
		}
		return result;
	}

	public void clearApkRecorder() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + APK_TABLE + ";");
		revertSeq(APK_TABLE);
	}

	public Cursor queryAllProcess() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(PROCESS_TABLE, PROCESS_COLUMNS, null, null, null, null, null);
		return c;
	}

	public Cursor queryProcess(String packageName) {
		Cursor result = null;
		if (!TextUtils.isEmpty(packageName)) {
			SQLiteDatabase db = getReadableDatabase();
			result = db.query(PROCESS_TABLE, PROCESS_COLUMNS, PACKAGE_NAME + " = ?", new String[] { packageName }, null,
					null, null);
		}
		return result;
	}

	public long insertProcess(ApkProcessInfo info) {
		long result = -1;
		if (info != null) {
			SQLiteDatabase db = getWritableDatabase();
			ArrayList<String> processNames = info.getProcessName();
			Log.d(this, "insertProcess=>packageName: " + info.getPackageName());
			for (int i = 0; i < processNames.size(); i++) {
				Log.d(this, "insertProcess=>process: " + processNames.get(i));
				ContentValues values = new ContentValues();
				values.put(PACKAGE_NAME, info.getPackageName());
				values.put(PROCESS_NAME, processNames.get(i));
				result = db.insert(PROCESS_TABLE, null, values);
			}
		}
		return result;
	}

	public void clearProcessRecorder() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + PROCESS_TABLE + ";");
		revertSeq(PROCESS_TABLE);
	}

}
