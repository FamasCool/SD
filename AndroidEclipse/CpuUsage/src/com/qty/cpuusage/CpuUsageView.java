package com.qty.cpuusage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CpuUsageView extends View {

	private static final boolean DEBUG = true;
	private static final String TAG = "CpuUsageView";
	
	private static final int MSG_REFRESH_CPU_USAGE = 0;

	private Resources mResources;
	private Paint mBackgroundPaint;
	private Paint mPathPaint;
	private Paint mTextPaint;
	private ArrayList<Path> mCpuUsagePath;
	private android.graphics.Path mPath;

	private int mCore;
	private int mBackgroundColor;
	private int mTextColor;
	private int mPathColor;
	private int mPaddingLeft;
	private int mPaddingTop;
	private int mPaddingRight;
	private int mPaddingBottom;
	private int mTextSize;
	private int mTextAlpha;
	private int mPathWidth;
	private int mPath1Length;
	private int mPath2Length;
	private int mPathStep;
	private int mRefreshTime;
	private long mBaseUserTime;
	private long mBaseSystemTime;
	private long mBaseIoWaitTime;
	private long mBaseIrqTime;
	private long mBaseSoftIrqTime;
	private long mBaseIdleTime;
	private int mRelUserTime;
	private int mRelSystemTime;
	private int mRelIoWaitTime;
	private int mRelIrqTime;
	private int mRelSoftIrqTime;
	private int mRelIdleTime;
	
	private String mCpuStatPath;
	
	private boolean mShowCpuText;
	private boolean mIsSizeChanged;

	public CpuUsageView(Context context) {
		this(context, null);
	}

	public CpuUsageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuUsageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mResources = getContext().getResources();
		
		mCpuStatPath = mResources.getString(R.string.cpu_stat_path);
		
		mCore = -1;
		mBackgroundColor = mResources.getColor(R.color.cpu_usage_view_background);
		mTextColor = mResources.getColor(R.color.cpu_usage_view_text_color);
		mPathColor = mResources.getColor(R.color.cpu_usage_view_path_color);
		mPaddingLeft = mResources.getInteger(R.integer.cpu_usage_view_padding_left);
		mPaddingTop = mResources.getInteger(R.integer.cpu_usage_view_padding_top);
		mPaddingRight = mResources.getInteger(R.integer.cpu_usage_view_padding_right);
		mPaddingBottom = mResources.getInteger(R.integer.cpu_usage_view_padding_bottom);
		mTextSize = mResources.getDimensionPixelSize(R.dimen.cpu_usage_text_size);
		mTextAlpha = mResources.getInteger(R.integer.cpu_usage_text_alpha);
		mPathWidth = mResources.getInteger(R.integer.cpu_usage_view_path_width);
		mPathStep = mResources.getInteger(R.integer.cpu_usage_view_path_step);
		mRefreshTime = mResources.getInteger(R.integer.cpu_usage_view_refresh_time);
		mPath1Length = 0;
		mPath2Length = 0;
		mBaseUserTime = 0;
		mBaseSystemTime = 0;
		mBaseIoWaitTime = 0;
		mBaseIrqTime = 0;
		mBaseSoftIrqTime = 0;
		mBaseIdleTime = 0;
		mRelSystemTime = 0;
		mRelIoWaitTime = 0;
		mRelIrqTime = 0;
		mRelSoftIrqTime = 0;
		mRelIdleTime = 0;
		
		mShowCpuText = mResources.getBoolean(R.bool.show_cpu_text);
		mIsSizeChanged = false;
		
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setColor(mBackgroundColor);
		mBackgroundPaint.setAntiAlias(true);
		mPathPaint = new Paint();
		mPathPaint.setColor(mPathColor);
		mPathPaint.setAntiAlias(true);
		mPathPaint.setStyle(Paint.Style.STROKE);
		mPathPaint.setStrokeWidth(mPathWidth);
		mTextPaint = new Paint();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setStyle(Paint.Style.STROKE);
		mTextPaint.setColor(mTextColor);
		mTextPaint.setTextSize(mTextSize);
		mTextPaint.setAlpha(mTextAlpha);
		mCpuUsagePath = new ArrayList<Path>();
		mPath = new android.graphics.Path();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (DEBUG)
			Log.d(TAG, "onAttachedToWindow()...");
		mHandler.sendEmptyMessage(MSG_REFRESH_CPU_USAGE);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()...");
		mHandler.removeMessages(MSG_REFRESH_CPU_USAGE);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredHeight = measureHeight(heightMeasureSpec);
		int measuredWidth = measureWidth(widthMeasureSpec);
		if (DEBUG)
			Log.d(TAG, "onMeasure=>width: " + measuredWidth + ", height: " + measuredHeight);
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (DEBUG) Log.d(TAG, "onSizeChanged=>w: " + w + ", h: " + h + ", oldw: " + oldw + ", oldh: " + oldh);
		mIsSizeChanged = true;
		updateCpuUsagePaths(w, h, oldw, oldh);
		mIsSizeChanged = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Rect background = new Rect(mPaddingLeft, mPaddingTop, getWidth() - mPaddingRight, getHeight() - mPaddingBottom);
		canvas.drawRect(background, mBackgroundPaint);
		
		if (!mPath.isEmpty()) {
			canvas.drawPath(mPath, mPathPaint);
		}

		if (mShowCpuText && mCore >= 0) {
			String coreText = "Cpu" + (mCore + 1);
			canvas.drawText(coreText, 3, mTextSize, mTextPaint);
		}
	}

	private int measureHeight(int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		int core = CpuUtils.getCpuCore();
		if (DEBUG)
			Log.d(TAG, "measureHeight=>mode: " + specMode + " size: " + specSize);
		int result = specSize;
		if (specMode == MeasureSpec.AT_MOST) {
			if (core > 4) {
				result = specSize / 2;
			} else {
				result = specSize;
			}
		} else if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		}
		return result;
	}

	private int measureWidth(int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		int core = CpuUtils.getCpuCore();
		if (DEBUG)
			Log.d(TAG, "measureWidth=>mode: " + specMode + " size: " + specSize);
		int result = 500;
		if (specMode == MeasureSpec.AT_MOST) {
			if (core > 0) {
				if (core > 4) {
					core = 4;
				}
				result = specSize / core;
			} else {
				result = specSize;
			}
		} else if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		}
		return result;
	}

	public void setCore(int core) {
		if (DEBUG) Log.d(TAG, "setCore=>core: " + core);
		mCore = core;
	}
	
	public void setCpuUsage(double usage) {
		if (DEBUG) {
			Log.d(TAG, "setCpuUsage=>usage: " + usage + ", isSizeChanged: " + mIsSizeChanged);
		}
		int width = getWidth() - mPaddingLeft - mPaddingRight;
		int height = getHeight() - mPaddingTop - mPaddingBottom;
		Path path = new Path();
		path.x = width;
		path.y = (int)(height - (height * usage));
		if (DEBUG) Log.d(TAG, "setCpuUsage=>x: " + path.x + ", y: " + path.y);
		if (mCpuUsagePath.size() <= 0) {
			mCpuUsagePath.add(path);
		} else {
			translationPath(-mPathStep, 0);
			mCpuUsagePath.add(path);
		}
		generatePath();
		invalidate();
	}
	
	public void updateCpuUsagePaths(int w, int h, int oldw, int oldh) {
		//if (DEBUG) Log.d(TAG, "updateCpuUsagePath()...");
		if (mCpuUsagePath.size() > 0 && oldw != 0 && oldh != 0) {
			Path path = null;
			for (int i = 0; i < mCpuUsagePath.size(); i++) {
				path = mCpuUsagePath.get(i);
				mCpuUsagePath.remove(i);
				path.x = (int)((double)path.x * (w / oldw));
				if (path.x >= -getWidth()) {
					path.y = (int)((double)path.y * (h / oldh));
					mCpuUsagePath.add(i, path);
				}
				//if (DEBUG) Log.d(TAG, "updateCpuUsagePaths=>i: " + i + ", x: " + path.x + ", y: " + path.y);
			}
		}
		generatePath();
		invalidate();
	}
	
	public void translationPath(int x, int y) {
		//if (DEBUG) Log.d(TAG, "translationPath=>x: " + x + ", y: " + y);
		if (mCpuUsagePath.size() > 0) {
			Path path = null;
			for (int i = 0; i < mCpuUsagePath.size(); i++) {
				path = mCpuUsagePath.get(i);
				mCpuUsagePath.remove(i);
				path.x += x;
				if (path.x >= -getWidth()) {
					if (path.y > 0) {
						path.y += y;
					}
					mCpuUsagePath.add(i, path);
				}
				//if (DEBUG) Log.d(TAG, "translationPath=>i: " + i + " x: " + path.x + ", y: " + path.y);
			}
		}
	}
	
	private void generatePath() {
		if (mCpuUsagePath.size() > 0) {
			mPath.reset();
			Path path = null;
			for (int i = 0; i < mCpuUsagePath.size(); i++) {
				path = mCpuUsagePath.get(i);
				if (i == 0) {
					mPath.moveTo(path.x, path.y);
				} else {
					mPath.lineTo(path.x, path.y);
				}
			}
		}
	}
	
	private void refreshCpuUsage() {
		if (mCore >= 0) {
			double usage = 0.0;
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(mCpuStatPath));
				String str = reader.readLine();
				while (str != null) {
					if (str.startsWith("cpu" + mCore)) {
						//if (DEBUG) Log.d(TAG, "refreshCpuUsage=>str: " + str);
						String[] strs = str.split(" ");
						if (strs.length >= 8) {
							long user = Long.parseLong(strs[1].trim());
							long nice = Long.parseLong(strs[2].trim());
							long system = Long.parseLong(strs[3].trim());
							long idle = Long.parseLong(strs[4].trim());
							long iowait = Long.parseLong(strs[5].trim());
							long irq = Long.parseLong(strs[6].trim());
							long softirq = Long.parseLong(strs[7].trim());
							mRelUserTime = (int)((user + nice) - mBaseUserTime);
							mRelSystemTime = (int)(system - mBaseSystemTime);
							mRelIoWaitTime = (int)(iowait - mBaseIoWaitTime);
							mRelIrqTime = (int)(irq - mBaseIrqTime);
							mRelSoftIrqTime = (int)(softirq - mBaseSoftIrqTime);
							mRelIdleTime = (int)(idle - mBaseIdleTime);
							int denom = mRelUserTime + mRelSystemTime + mRelIrqTime + mRelIdleTime;
							if (denom > 0) {
								usage = ((double)(mRelUserTime+mRelSystemTime+mRelIrqTime)) / denom;
							}
							 mBaseUserTime = user + nice;
					         mBaseSystemTime = system;
					         mBaseIoWaitTime = iowait;
					         mBaseIrqTime = irq;
					         mBaseSoftIrqTime = softirq;
					         mBaseIdleTime = idle;
						}
						break;
					} else {
						str = reader.readLine();
					}
				}
				if (DEBUG) Log.d(TAG, "refreshCpuUsage=>core: " + mCore + ", usage: " + usage);
				setCpuUsage(usage);
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
		}
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (DEBUG) Log.d(TAG, "handleMessage=>what: " + msg.what);
			switch (msg.what) {
			case MSG_REFRESH_CPU_USAGE:
				refreshCpuUsage();
				mHandler.sendEmptyMessageDelayed(MSG_REFRESH_CPU_USAGE, mRefreshTime);
				break;
			}
		}
	};
	
	class Path {
		public int x;
		public int y;
	}

}
