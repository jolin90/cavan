package com.cavan.cavanmain;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.cavan.android.CavanAndroid;
import com.cavan.android.CavanPackageName;
import com.cavan.android.FloatWidowService;
import com.cavan.java.CavanTimedArray;

public class FloatMessageService extends FloatWidowService {

	public static final int TEXT_PADDING = 8;
	public static final float TEXT_SIZE_TIME = 16;
	public static final float TEXT_SIZE_MESSAGE = 12;
	public static final int TEXT_COLOR_TIME = Color.WHITE;
	public static final int TEXT_COLOR_MESSAGE = Color.YELLOW;

	private int mLastSecond;
	private boolean mUserPresent;
	private TextView mTextViewTime;
	private TextView mTextViewAutoUnlock;
	private CavanTimedArray<String> mNetSharedCodes = new CavanTimedArray<String>(3600000);
	private HashMap<CharSequence, RedPacketCode> mMessageCodeMap = new HashMap<CharSequence, RedPacketCode>();

	private UdpClientThread mShareThread;
	private Handler mHandler = new Handler();
	private InputMethodPickerRunnable mInputMethodPickerRunnable = new InputMethodPickerRunnable();

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
				if (getTextCount() > 0 || CavanAndroid.isPreferenceEnabled(FloatMessageService.this, MainActivity.KEY_AUTO_UNLOCK)) {
					unlockScreen();
				}
				break;

			case Intent.ACTION_USER_PRESENT:
				mTextViewAutoUnlock.setVisibility(View.INVISIBLE);
				mUserPresent = true;
				break;
			}
		}
	};

	private IFloatMessageService.Stub mBinder = new IFloatMessageService.Stub() {

		private void sendCodeUpdateBroadcast(String action, RedPacketCode code) {
			Intent intent = new Intent(action);
			intent.putExtra("code", code);
			sendBroadcast(intent);
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
		public int addMessage(CharSequence message, RedPacketCode code) throws RemoteException {
			TextView view = (TextView) FloatMessageService.this.addText(message, -1);
			if (view == null) {
				return -1;
			}

			unlockScreen();

			if (code != null) {
				mMessageCodeMap.put(message, code);
				mInputMethodPickerRunnable.post();

				sendCodeUpdateBroadcast(MainActivity.ACTION_CODE_ADD, code);

				if (code.isNetShared()) {
					mNetSharedCodes.addTimedValue(code.getCode());
				} else {
					sendSharedCode(code.getCode());
				}
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

			RedPacketCode code = mMessageCodeMap.get(message);
			if (code != null) {
				mMessageCodeMap.remove(message);
				sendCodeUpdateBroadcast(MainActivity.ACTION_CODE_REMOVE, code);
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
		public int getMessageCount() throws RemoteException {
			return FloatMessageService.this.getTextCount();
		}

		@Override
		public List<RedPacketCode> getCodes() throws RemoteException {
			List<RedPacketCode> codes = new ArrayList<RedPacketCode>();

			for (RedPacketCode code : mMessageCodeMap.values()) {
				codes.add(code);
			}

			return codes;
		}

		@Override
		public int getCodeCount() throws RemoteException {
			return mMessageCodeMap.size();
		}

		@Override
		public boolean sendSharedCode(String code) throws RemoteException {
			if (mShareThread == null || mNetSharedCodes.hasTimedValue(code)) {
				return false;
			}

			return mShareThread.sendCode(code);
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

	public static Intent buildIntent(Context context) {
		return new Intent(context, FloatMessageService.class);
	}

	public static Intent startService(Context context) {
		Intent intent = buildIntent(context);
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

		mShareThread = new UdpClientThread();
		mShareThread.start();

		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (mShareThread != null) {
			mShareThread.quit();
		}

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

		if (MainActivity.isFloatTimerEnabled(this)) {
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

	public class InputMethodPickerRunnable implements Runnable {

		private boolean mAlipayRunning;

		public void post() {
			mAlipayRunning = false;
			mHandler.post(this);
		}

		@Override
		public void run() {
			if (getTextCount() <= 0) {
				return;
			}

			if (CavanInputMethod.isDefaultInputMethod(getApplicationContext())) {
				return;
			}

			if (mAlipayRunning) {
				InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				manager.showInputMethodPicker();
			} else {
				ComponentName info = CavanAndroid.getTopActivityInfo(getApplicationContext());
				if (info != null && CavanPackageName.ALIPAY.equals(info.getPackageName())) {
					mAlipayRunning = true;
				}

				mHandler.removeCallbacks(this);
				mHandler.postDelayed(this, 500);
			}
		}
	}

	public class UdpClientThread extends HandlerThread implements Callback {

		private Handler mHandler;
		private MulticastSocket mSocket;

		public UdpClientThread() {
			super("UdpClientThread");
		}

		public boolean sendCode(String code) {
			if (mHandler == null) {
				return false;
			}

			if (MainActivity.isLanShareEnabled(FloatMessageService.this)) {
				mHandler.obtainMessage(0, code).sendToTarget();
				return true;
			}

			return false;
		}

		@Override
		public boolean quit() {
			boolean result = super.quit();

			if (mSocket != null) {
				mSocket.close();
				mSocket = null;
			}

			return result;
		}

		@Override
		protected void onLooperPrepared() {
			mHandler = new Handler(getLooper(), this);
		}

		@Override
		public boolean handleMessage(Message msg) {
			String code = (String) msg.obj;
			byte[] bytes = (RedPacketListenerService.LAN_SHARE_PREFIX + code).getBytes();

			CavanAndroid.eLog("send" + msg.what + " = " + code);

			try {
				if (mSocket == null) {
					mSocket = new MulticastSocket();
				}

				DatagramPacket pack = new DatagramPacket(bytes, bytes.length,
						InetAddress.getByName(RedPacketListenerService.LAN_SHARE_ADDR),
						RedPacketListenerService.LAN_SHARE_PORT);

				mSocket.send(pack);
			} catch (Exception e) {
				e.printStackTrace();

				if (mSocket != null) {
					mSocket.close();
					mSocket = null;
				}
			}

			if (msg.what < 10) {
				int index = msg.what + 1;

				Message message = mHandler.obtainMessage(index, code);
				mHandler.sendMessageDelayed(message, index * 100);
			}

			return true;
		}
	}
}
