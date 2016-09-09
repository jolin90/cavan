package com.cavan.cavanmain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.cavan.android.CavanAndroid;
import com.cavan.android.FloatWidowService;

public class FloatMessageService extends FloatWidowService {

	public static final String ACTION_CODE_UPDATED = "cavan.intent.action.ACTION_CODE_UPDATED";

	public static final int TEXT_PADDING = 8;
	public static final float TEXT_SIZE_TIME = 16;
	public static final float TEXT_SIZE_MESSAGE = 12;
	public static final int TEXT_COLOR_TIME = Color.WHITE;
	public static final int TEXT_COLOR_MESSAGE = Color.YELLOW;

	private static final int MSG_SHOW_INPUT_METHOD_PICKER = 1;

	private int mLastSecond;
	private boolean mUserPresent;
	private TextView mTextViewTime;
	private TextView mTextViewAutoUnlock;
	private List<String> mCodeList = new ArrayList<String>();
	private HashMap<String, Integer> mCodeDelayMap = new HashMap<String, Integer>();

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SHOW_INPUT_METHOD_PICKER:
				InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				manager.showInputMethodPicker();
				break;
			}
		}
	};

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			CavanAndroid.eLog("action = " + action);

			switch (action) {
			case Intent.ACTION_SCREEN_OFF:
				CavanAndroid.setLockScreenEnable(FloatMessageService.this, true);
				mUserPresent = false;
				break;

			case Intent.ACTION_SCREEN_ON:
				if (getTextCount() <= 0) {
					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(FloatMessageService.this);
					if (preferences == null) {
						break;
					}

					if (!preferences.getBoolean(MainActivity.KEY_AUTO_UNLOCK, false)) {
						break;
					}
				}

				unlockScreen();
				break;

			case Intent.ACTION_USER_PRESENT:
				mTextViewAutoUnlock.setVisibility(View.INVISIBLE);
				mUserPresent = true;
				break;
			}
		}
	};

	private IFloatMessageService.Stub mBinder = new IFloatMessageService.Stub() {

		private void sendCodeUpdateBroadcast() {
			sendBroadcast(new Intent(ACTION_CODE_UPDATED));
		}

		@Override
		public boolean setTimerEnable(boolean enable) throws RemoteException {
			return FloatMessageService.this.setTimerEnable(enable);
		}

		@Override
		public boolean getTimerState() throws RemoteException {
			return mTextViewTime != null && mTextViewTime.getVisibility() != View.INVISIBLE;
		}

		@Override
		public int addMessage(CharSequence message, CharSequence code, int delay) throws RemoteException {
			TextView view = (TextView) FloatMessageService.this.addText(message, -1);
			if (view == null) {
				return -1;
			}

			unlockScreen();

			if (code != null) {
				String text = code.toString();
				mCodeList.add(text);
				mCodeDelayMap.put(text, delay);

				String method = CavanAndroid.getDefaultInputMethod(getApplicationContext());
				if ("com.cavan.cavanmain/.CavanInputMethod".equals(method) == false) {
					mHandler.sendEmptyMessageDelayed(MSG_SHOW_INPUT_METHOD_PICKER, 500);
				}

				sendCodeUpdateBroadcast();
			}

			return view.getId();
		}

		@Override
		public boolean hasMessage(CharSequence message) throws RemoteException {
			return FloatMessageService.this.hasText(message);
		}

		@Override
		public void removeMessage(CharSequence message) throws RemoteException {
			FloatMessageService.this.removeText(message);

			if (getTextCount() == 0) {
				mCodeList.clear();
				mCodeDelayMap.clear();
				sendCodeUpdateBroadcast();
			}
		}

		@Override
		public List<String> getMessages() throws RemoteException {
			List<String> list = new ArrayList<String>();
			for (CharSequence text : getTextSet()) {
				list.add(text.toString());
			}

			return list;
		}

		@Override
		public List<String> getCodes() throws RemoteException {
			return mCodeList;
		}

		@Override
		public int getCodeDelay(String code) throws RemoteException {
			Integer delay = mCodeDelayMap.get(code);
			if (delay == null || delay < 0) {
				return 0;
			}

			return delay;
		}
	};

	private Runnable mRunnableTime = new Runnable() {

		@Override
		public void run() {
			Calendar calendar = Calendar.getInstance();
			int second = calendar.get(Calendar.SECOND);
			if (second == mLastSecond) {
				mTextViewTime.postDelayed(this, 100);
			} else {
				mLastSecond = second;
				mTextViewTime.postDelayed(this, 1000);
				mTextViewTime.setText(getTimeText(calendar, second));
			}
		}
	};

	public static Intent startService(Context context) {
		Intent intent = new Intent(context, FloatMessageService.class);
		context.startService(intent);
		return intent;
	}

	public boolean unlockScreen() {
		if (mUserPresent) {
			return false;
		}

		mTextViewAutoUnlock.setVisibility(View.VISIBLE);
		CavanAndroid.setLockScreenEnable(FloatMessageService.this, false);

		return true;
	}

	public String getTimeText(Calendar calendar, int second) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);

		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	public String getTimeText(Calendar calendar) {
		return getTimeText(calendar, calendar.get(Calendar.SECOND));
	}

	public String getTimeText() {
		return getTimeText(Calendar.getInstance());
	}

	public boolean setTimerEnable(boolean enable) {
		if (mTextViewTime == null) {
			return false;
		}

		if (enable) {
			mLastSecond = -1;
			mTextViewTime.setVisibility(View.VISIBLE);
			mTextViewTime.post(mRunnableTime);
		} else {
			mTextViewTime.removeCallbacks(mRunnableTime);
			mTextViewTime.setVisibility(View.INVISIBLE);
		}

		return true;
	}

	public void initTextView(TextView view, CharSequence text) {
		if (text != null) {
			view.setText(text);
		}

		view.setMaxLines(1);
		view.setPadding(TEXT_PADDING, 0, TEXT_PADDING, 0);
		view.setBackgroundResource(R.drawable.desktop_timer_bg);
	}

	@Override
	protected View createRootView() {
		return View.inflate(this, R.layout.float_message, null);
	}

	@Override
	protected LayoutParams createRootViewLayoutParams() {
		LayoutParams params = super.createRootViewLayoutParams();
		if (params == null) {
			return null;
		}

		params.width = LayoutParams.MATCH_PARENT;

		return params;
	}

	@Override
	public void onCreate() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(mReceiver, filter );
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		setTimerEnable(false);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	protected View createView(CharSequence text) {
		TextView view = new TextView(getApplicationContext());

		initTextView(view, text);

		view.setTextSize(TEXT_SIZE_MESSAGE);
		view.setTextColor(TEXT_COLOR_MESSAGE);
		view.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

		return view;
	}

	@Override
	protected CharSequence getViewText(View arg0) {
		TextView view = (TextView) arg0;
		return view.getText();
	}

	@Override
	protected ViewGroup findViewGroup() {
		return (ViewGroup) findViewById(R.id.layoutMessage);
	}

	@Override
	protected boolean doInitialize() {
		mTextViewTime = (TextView) findViewById(R.id.textViewTime);

		initTextView(mTextViewTime, getTimeText());
		mTextViewTime.setTextSize(TEXT_SIZE_TIME);
		mTextViewTime.setTextColor(TEXT_COLOR_TIME);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences != null && preferences.getBoolean(MainActivity.KEY_FLOAT_TIMER, false)) {
			setTimerEnable(true);
		} else {
			setTimerEnable(false);
		}

		mTextViewAutoUnlock = (TextView) findViewById(R.id.textViewAutoUnlock);

		initTextView(mTextViewAutoUnlock, null);
		mTextViewAutoUnlock.setTextSize(TEXT_SIZE_TIME);
		mTextViewAutoUnlock.setTextColor(TEXT_COLOR_TIME);

		return super.doInitialize();
	}
}
