package com.cavan.cavanmain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.FileObserver;
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
import android.widget.TextView;

import com.cavan.android.CavanAndroid;
import com.cavan.android.CavanWakeLock;
import com.cavan.android.FloatWidowService;
import com.cavan.cavanjni.CavanJni;
import com.cavan.java.CavanString;

@SuppressWarnings("deprecation")
public class FloatMessageService extends FloatWidowService {

	public static final long KEEP_ALIVE_DELAY = 120000;

	public static final String NET_CMD_RDPKG = "RedPacketCode: ";
	public static final String NET_CMD_UPDATE = "RedPacketUpdate: ";
	public static final String NET_CMD_DELETE = "RedPacketDelete: ";
	public static final String NET_CMD_TEST = "CavanNetworkTest";
	public static final String NET_CMD_KEEP_ALIVE = "CavanKeepAlive";
	public static final String NET_CMD_TM_CODE = "SecretOrder: ";
	public static final String NET_CMD_CLIPBOARD = "Clipboard: ";

	public static final String PATH_QQ_IMAGES = Environment.getExternalStorageDirectory().getPath() + "/Tencent/QQ_Images";

	public static final int UDP_PORT = 9898;
	public static final String UDP_ADDR = "224.0.0.1";

	public static final int TEXT_PADDING = 8;
	public static final float TEXT_SIZE_TIME = 16;
	public static final float TEXT_SIZE_MESSAGE = 12;
	public static final int TEXT_COLOR_TIME = Color.WHITE;
	public static final int TEXT_COLOR_MESSAGE = Color.YELLOW;

	private static final int MSG_UPDATE_TIME = 0;
	private static final int MSG_SHOW_TOAST = 1;
	private static final int MSG_TCP_SERVICE_STATE_CHANGED = 2;
	private static final int MSG_TCP_SERVICE_UPDATED = 3;
	private static final int MSG_TCP_BRIDGE_STATE_CHANGED = 5;
	private static final int MSG_TCP_BRIDGE_UPDATED = 6;
	private static final int MSG_START_OCR = 7;
	private static final int MSG_CLIPBOARD_RECEIVED = 8;
	private static final int MSG_CHECK_KEYGUARD = 9;
	private static final int MSG_RED_PACKET_UPDATED = 10;
	private static final int MSG_KEEP_ALIVE = 11;
	private static final int MSG_CHECK_SERVICE_STATE = 12;

	private int mLastSecond;
	private boolean mScreenClosed;
	private TextView mTextViewTime;
	private CavanWakeLock mWakeLock = new CavanWakeLock(FloatMessageService.class.getCanonicalName());
	private HashMap<CharSequence, RedPacketCode> mMessageCodeMap = new HashMap<CharSequence, RedPacketCode>();

	private UdpDaemonThread mUdpDaemon;
	private TcpDaemonThread mTcpDaemon;
	private TcpBridgeThread mTcpBridge;
	private NetworkSendThread mNetSender;

	private boolean mNetworkConnected;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_TIME:
				removeMessages(MSG_UPDATE_TIME);

				if (mScreenClosed || mTextViewTime.getVisibility() != View.VISIBLE) {
					break;
				}

				Calendar calendar = Calendar.getInstance();
				int second = calendar.get(Calendar.SECOND);
				if (second == mLastSecond) {
					sendEmptyMessageDelayed(MSG_UPDATE_TIME, 100);
				} else {
					mLastSecond = second;
					sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
					mTextViewTime.setText(getTimeText(calendar, second));
				}
				break;

			case MSG_TCP_SERVICE_STATE_CHANGED:
				Intent intent = new Intent(MainActivity.ACTION_WAN_UPDATED);
				intent.putExtra("state", msg.arg1);
				intent.putExtra("summary", (String) msg.obj);
				sendStickyBroadcast(intent);

				if (msg.arg1 == R.string.wan_connected) {
					if (mTcpDaemon != null) {
						mTcpDaemon.setConnDelay(0);
					}

					CavanAndroid.showToast(getApplicationContext(), msg.arg1);
				}
				break;

			case MSG_TCP_SERVICE_UPDATED:
				if (mNetworkConnected && MainActivity.isWanShareEnabled(FloatMessageService.this)) {
					if (mTcpDaemon == null) {
						mTcpDaemon = new TcpDaemonThread();
						mTcpDaemon.start();
					} else {
						mTcpDaemon.reload();
					}
				} else if (mTcpDaemon != null) {
					mTcpDaemon.setActive(false);
				}
				break;

			case MSG_TCP_BRIDGE_STATE_CHANGED:
				intent = new Intent(MainActivity.ACTION_BRIDGE_UPDATED);
				intent.putExtra("state", msg.arg1);
				sendStickyBroadcast(intent);

				// CavanAndroid.showToast(getApplicationContext(), msg.arg1);
				break;

			case MSG_TCP_BRIDGE_UPDATED:
				if (mNetworkConnected && MainActivity.isTcpBridgeEnabled(FloatMessageService.this)) {
					if (mTcpBridge == null) {
						mTcpBridge = new TcpBridgeThread();
						mTcpBridge.start();
					} else {
						synchronized (mTcpBridge) {
							mTcpBridge.setActive(true);
							mTcpBridge.killCommand();
							mTcpBridge.notify();
						}
					}
				} else if (mTcpBridge != null) {
					mTcpBridge.setActive(false);
				}
				break;

			case MSG_SHOW_TOAST:
				if (msg.obj instanceof String) {
					CavanAndroid.showToast(getApplicationContext(), (String) msg.obj);
				} else {
					CavanAndroid.showToast(getApplicationContext(), (int) msg.obj);
				}
				break;

			case MSG_START_OCR:
				MainActivity.startSogouOcrActivity(getApplicationContext());
				break;

			case MSG_CLIPBOARD_RECEIVED:
				String code = (String) msg.obj;
				String text = getResources().getString(R.string.clipboard_updated, code);
				CavanAndroid.showToast(getApplicationContext(), text);
				CavanAndroid.postClipboardTextTemp(getApplicationContext(), code);
				break;

			case MSG_CHECK_KEYGUARD:
				removeMessages(MSG_CHECK_KEYGUARD);

				if (mScreenClosed) {
					break;
				}

				if (CavanAndroid.inKeyguardRestrictedInputMode(FloatMessageService.this)) {
					sendEmptyMessageDelayed(MSG_CHECK_KEYGUARD, 2000);
				} else {
					mTextViewTime.setBackgroundResource(R.drawable.desktop_timer_bg);
				}
				break;

			case MSG_RED_PACKET_UPDATED:
				RedPacketCode node = (RedPacketCode) msg.obj;
				text = getResources().getString(R.string.red_packet_code_updated, node.getCode());
				CavanAndroid.showToast(getApplicationContext(), text);
				break;

			case MSG_KEEP_ALIVE:
				removeMessages(MSG_KEEP_ALIVE);

				if (mTcpDaemon != null && mTcpDaemon.isRunning()) {
					CavanAndroid.dLog("Send: " + NET_CMD_KEEP_ALIVE);

					mTcpDaemon.sendCommand(NET_CMD_KEEP_ALIVE);
					sendEmptyMessageDelayed(MSG_KEEP_ALIVE, KEEP_ALIVE_DELAY);
				}
				break;

			case MSG_CHECK_SERVICE_STATE:
				removeMessages(MSG_CHECK_SERVICE_STATE);
				if (checkServiceState()) {
					break;
				}

				sendEmptyMessageDelayed(MSG_CHECK_SERVICE_STATE, 5000);
				break;
			}
		}
	};

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			CavanAndroid.dLog("action = " + action);

			switch (action) {
			case Intent.ACTION_SCREEN_OFF:
				if (MainActivity.isDisableKeyguardEnabled(getApplicationContext())) {
					CavanAndroid.acquireWakeLock(getApplicationContext(), 5000);
					CavanAndroid.startActivity(getApplicationContext(), KeyguardActivity.class);
				}

				setLockScreenEnable(true);
				mScreenClosed = true;
				break;

			case Intent.ACTION_SCREEN_ON:
				mScreenClosed = false;
				mHandler.sendEmptyMessage(MSG_UPDATE_TIME);


				if (getTextCount() > 0 || MainActivity.isAutoUnlockEnabled(getApplicationContext())) {
					setLockScreenEnable(false);
				}

				mTextViewTime.setBackgroundResource(R.drawable.desktop_timer_unlock_bg);

				if (MainActivity.isAutoUnlockEnabled(getApplicationContext())) {
					mHandler.sendEmptyMessage(MSG_CHECK_KEYGUARD);
				}
				break;

			case ConnectivityManager.CONNECTIVITY_ACTION:
				updateNetworkConnState();
				break;

			case MainActivity.ACTION_SEND_WAN_COMMAN:
				if (mNetSender != null) {
					String command = intent.getStringExtra("command");
					mNetSender.sendTcpCommand(command, 0);
				}
				break;

			case MainActivity.ACTION_SERVICE_EXIT:
				String service = intent.getStringExtra("service");
				if (service == null) {
					break;
				}

				if (service.equals(CavanAccessibilityService.class.getCanonicalName())) {
					CavanAccessibilityService.checkAndOpenSettingsActivity(getApplicationContext());
				} else if (service.equals(RedPacketListenerService.class.getCanonicalName())) {
					RedPacketListenerService.checkAndOpenSettingsActivity(getApplicationContext());
				}
				break;
			}
		}
	};

	private FileObserver mFileObserverQQ = new FileObserver(PATH_QQ_IMAGES, FileObserver.CLOSE_WRITE) {

		private String mPath;

		@Override
		public void onEvent(int event, String path) {
			if (path.equals(mPath)) {
				path = mPath;
			} else {
				mPath = path;
			}

			mHandler.removeMessages(MSG_START_OCR, path);
			Message message = mHandler.obtainMessage(MSG_START_OCR, path);
			mHandler.sendMessageDelayed(message, 200);
		}
	};

	private IFloatMessageService.Stub mBinder = new IFloatMessageService.Stub() {

		private void sendCodeUpdateBroadcast(String action, String code) {
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
			return mTextViewTime != null && mTextViewTime.getVisibility() == View.VISIBLE;
		}

		@Override
		public int addMessage(CharSequence message, String code) throws RemoteException {
			setLockScreenEnable(false);
			CavanAndroid.acquireWakeupLock(getApplicationContext(), 20000);

			if (code != null) {
				RedPacketCode node = RedPacketCode.getInstence(code);
				if (node != null) {
					if (node.isTestOnly()) {
						sendCodeUpdateBroadcast(MainActivity.ACTION_CODE_TEST, code);
					} else {
						mMessageCodeMap.put(message, node);

						sendCodeUpdateBroadcast(MainActivity.ACTION_CODE_ADD, code);

						if (node.isSendEnabled() && mNetSender != null) {
							long delay = node.getTime() - System.currentTimeMillis();
							mNetSender.sendCode(code, delay);
						}
					}
				}
			}

			FloatMessageService.this.removeText(message);
			TextView view = (TextView) FloatMessageService.this.addText(message, -1);
			if (view == null) {
				return -1;
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
				code.setNetworkEnable();
				mMessageCodeMap.remove(message);
				sendCodeUpdateBroadcast(MainActivity.ACTION_CODE_REMOVE, code.getCode());
			}
		}

		@Override
		public void removeMessageAll() throws RemoteException {
			FloatMessageService.this.removeTextAll();
			mMessageCodeMap.clear();
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
		public List<String> getCodes() throws RemoteException {
			List<String> codes = new ArrayList<String>();

			for (RedPacketCode code : mMessageCodeMap.values()) {
				codes.add(code.getCode());
			}

			return codes;
		}

		@Override
		public int getCodeCount() throws RemoteException {
			return mMessageCodeMap.size();
		}

		@Override
		public int getCodePending() throws RemoteException {
			int count = 0;

			for (RedPacketCode code : mMessageCodeMap.values()) {
				if (!code.isCompleted()) {
					count++;
				}
			}

			return count;
		}

		@Override
		public void updateTcpService() throws RemoteException {
			mHandler.sendEmptyMessageDelayed(MSG_TCP_SERVICE_UPDATED, 500);
		}

		@Override
		public void updateTcpBridge() throws RemoteException {
			mHandler.sendEmptyMessageDelayed(MSG_TCP_BRIDGE_UPDATED, 500);
		}

		@Override
		public boolean sendRedPacketCode(String code) throws RemoteException {
			if (mNetSender == null) {
				return false;
			}

			return mNetSender.sendCode(code, 0);
		}

		@Override
		public boolean sendUdpCommand(String command) throws RemoteException {
			if (mNetSender == null) {
				return false;
			}

			return mNetSender.sendUdpCommand(command, 0, 0);
		}

		@Override
		public boolean sendTcpCommand(String command) throws RemoteException {
			if (mNetSender == null) {
				return false;
			}

			return mNetSender.sendTcpCommand(command, 0);
		}

		@Override
		public boolean isSuspendDisabled() throws RemoteException {
			return FloatMessageService.this.isSuspendDisabled();
		}

		@Override
		public void setSuspendDisable(boolean disable) throws RemoteException {
			FloatMessageService.this.setSuspendDisable(disable);
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

	private boolean checkServiceState() {
		return CavanAccessibilityService.checkAndOpenSettingsActivity(this) && RedPacketListenerService.checkAndOpenSettingsActivity(this);
	}

	public boolean isSuspendDisabled() {
		return mWakeLock.isHeld();
	}

	public void setSuspendDisable(boolean disable) {
		if (disable) {
			mWakeLock.acquire(this);
		} else {
			mWakeLock.release();
		}
	}

	public boolean setLockScreenEnable(boolean enable) {
		if (MainActivity.isDisableKeyguardEnabled(this)) {
			enable = true;
		}

		CavanAndroid.setLockScreenEnable(this, enable);

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
			mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
		} else {
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
	}

	private void onNetworkCommandReceived(String type, String command) {
		CavanAndroid.dLog("receive = " + command);

		if (command.equals(NET_CMD_TEST)) {
			command = getResources().getString(R.string.network_test_success, type);
			mHandler.obtainMessage(MSG_SHOW_TOAST, command).sendToTarget();
		} else if (command.startsWith(NET_CMD_RDPKG)) {
			String code = CavanString.deleteSpace(command.substring(NET_CMD_RDPKG.length()));

			CavanAndroid.dLog("code = " + code);

			if (code.equals(NET_CMD_TEST)) {
				command = getResources().getString(R.string.network_test_success, type);
				mHandler.obtainMessage(MSG_SHOW_TOAST, command).sendToTarget();
			} else if (MainActivity.isRedPacketCodeReceiveEnabled()) {
				RedPacketCode node = RedPacketCode.getInstence(code);
				if (node == null || node.isRecvEnabled()) {
					Intent intent = new Intent(MainActivity.ACTION_CODE_RECEIVED);
					intent.putExtra("type", type);
					intent.putExtra("code", code);
					intent.putExtra("shared", true);
					sendBroadcast(intent);
				}
			}
		} else if (command.startsWith(NET_CMD_UPDATE)) {
			String text = command.substring(NET_CMD_UPDATE.length());
			String[] texts = text.split("\\s*\\|\\s*");

			if (texts.length == 3) {
				try {
					String code = texts[0].trim();
					long time = Long.parseLong(texts[1]);
					boolean ignore = Boolean.parseBoolean(texts[2]);

					RedPacketCode node = RedPacketCode.update(this, code, time, ignore);
					if (node != null) {
						mHandler.obtainMessage(MSG_RED_PACKET_UPDATED, node).sendToTarget();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (command.startsWith(NET_CMD_TM_CODE)) {
			String code = CavanString.deleteSpace(command.substring(NET_CMD_TM_CODE.length()));
			mHandler.obtainMessage(MSG_CLIPBOARD_RECEIVED, code).sendToTarget();
		} else if (command.startsWith(NET_CMD_CLIPBOARD)) {
			String code = command.substring(NET_CMD_CLIPBOARD.length());
			mHandler.obtainMessage(MSG_CLIPBOARD_RECEIVED, code).sendToTarget();
		}
	}

	public void updateNetworkConnState() {
		mNetworkConnected = CavanAndroid.isNetworkAvailable(this);
		mHandler.sendEmptyMessage(MSG_TCP_SERVICE_UPDATED);
		mHandler.sendEmptyMessage(MSG_TCP_BRIDGE_UPDATED);
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
		super.onCreate();

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(MainActivity.ACTION_SEND_WAN_COMMAN);
		filter.addAction(MainActivity.ACTION_SERVICE_EXIT);
		registerReceiver(mReceiver, filter );

		mFileObserverQQ.startWatching();

		mNetSender = new NetworkSendThread();
		mNetSender.start();

		mUdpDaemon = new UdpDaemonThread();
		mUdpDaemon.start();

		updateNetworkConnState();
		setSuspendDisable(MainActivity.isDisableSuspendEnabled(this));

		mHandler.sendEmptyMessage(MSG_CHECK_SERVICE_STATE);
	}

	@Override
	public void onDestroy() {
		if (mTcpDaemon != null) {
			mTcpDaemon.setActive(false);
		}

		if (mUdpDaemon != null) {
			mUdpDaemon.setActive(false);
		}

		if (mNetSender != null) {
			mNetSender.quit();
		}

		mFileObserverQQ.stopWatching();
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
		view.setBackgroundResource(R.drawable.desktop_timer_unlock_bg);

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
		mTextViewTime.setBackgroundResource(R.drawable.desktop_timer_bg);
		mTextViewTime.setTextSize(TEXT_SIZE_TIME);
		mTextViewTime.setTextColor(TEXT_COLOR_TIME);

		if (MainActivity.isFloatTimerEnabled(this)) {
			setTimerEnable(true);
		} else {
			setTimerEnable(false);
		}

		return super.doInitialize();
	}

	public class NetworkSendThread extends HandlerThread implements Callback {

		public static final int MSG_SEND_UDP_COMMAND = 2;
		public static final int MSG_SEND_TCP_COMMAND = 3;

		private Handler mNetSendHandler;
		private MulticastSocket mUdpSocket;

		public NetworkSendThread() {
			super("NetworkSendThread");
		}

		public boolean sendCommand(int what, String command, long delay, int retry) {
			if (mNetSendHandler == null) {
				return false;
			}

			Message message = mNetSendHandler.obtainMessage(what, retry, 0, command);

			if (delay > 0) {
				mNetSendHandler.sendMessageDelayed(message, delay);
			} else {
				mNetSendHandler.sendMessage(message);
			}

			return true;
		}

		public boolean sendTcpCommand(String command, long delay) {
			if (MainActivity.isWanShareEnabled(getApplicationContext())) {
				return sendCommand(MSG_SEND_TCP_COMMAND, command, delay, 0);
			}

			return false;
		}

		public boolean sendUdpCommand(String command, long delay, int retry) {
			if (MainActivity.isLanShareEnabled(getApplicationContext())) {
				return sendCommand(MSG_SEND_UDP_COMMAND, command, delay, retry);
			}

			return false;
		}

		public boolean sendCode(String code, long delay) {
			String command = NET_CMD_RDPKG + code;
			return sendTcpCommand(command, delay) || sendUdpCommand(command, delay, 5);
		}

		@Override
		public boolean quit() {
			boolean result = super.quit();

			if (mUdpSocket != null) {
				mUdpSocket.close();
				mUdpSocket = null;
			}

			return result;
		}

		@Override
		protected void onLooperPrepared() {
			mNetSendHandler = new Handler(getLooper(), this);
		}

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SEND_TCP_COMMAND:
				if (mTcpDaemon != null) {
					String command = (String) msg.obj;

					if (mTcpDaemon.sendCommand(command)) {
						CavanAndroid.dLog("tcp success send: " + command);
					}
				}
				break;

			case MSG_SEND_UDP_COMMAND:
				try {
					if (mUdpSocket == null) {
						mUdpSocket = new MulticastSocket();
					}

					String command = (String) msg.obj;
					byte[] bytes = command.getBytes();
					DatagramPacket pack = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(UDP_ADDR), UDP_PORT);

					mUdpSocket.send(pack);
					CavanAndroid.dLog("udp success send: " + command);
				} catch (Exception e) {
					e.printStackTrace();

					if (mUdpSocket != null) {
						mUdpSocket.close();
						mUdpSocket = null;
					}
				}

				if (msg.arg1 > 0) {
					Message message = mNetSendHandler.obtainMessage(msg.what, msg.arg1 - 1, 0, msg.obj);
					mNetSendHandler.sendMessageDelayed(message, 1000);
				}
				break;
			}

			return true;
		}
	}

	public class UdpDaemonThread extends Thread {

		private boolean mActive;
		private MulticastSocket mSocket;

		public void setActive(boolean enable) {
			if (enable) {
				mActive = true;
			} else {
				mActive = false;

				if (mSocket != null) {
					MulticastSocket socket = mSocket;

					mSocket = null;
					socket.close();
				}
			}
		}

		@Override
		public void run() {
			mActive = true;
			CavanAndroid.setMulticastEnabled(getApplicationContext(), true);

			while (mActive) {
				if (mSocket != null) {
					mSocket.close();
				}

				try {
					mSocket = new MulticastSocket(UDP_PORT);
					mSocket.joinGroup(InetAddress.getByName(UDP_ADDR));
				} catch (IOException e) {
					e.printStackTrace();

					synchronized (this) {
						try {
							wait(2000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}

					continue;
				}

				CavanAndroid.dLog("UdpServiceThread running");

				byte[] bytes = new byte[128];
				DatagramPacket pack = new DatagramPacket(bytes, bytes.length);

				while (mActive) {
					try {
						mSocket.receive(pack);

						if (MainActivity.isLanShareEnabled(FloatMessageService.this)) {
							String text = new String(pack.getData(), 0, pack.getLength());
							onNetworkCommandReceived("内网分享", text);
						}
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}

				CavanAndroid.dLog("UdpServiceThread stopping");
			}

			mUdpDaemon = null;

			if (mSocket != null) {
				mSocket.close();
				mSocket = null;
			}

			CavanAndroid.setMulticastEnabled(getApplicationContext(), false);
			mActive = false;
		}
	}

	public class TcpDaemonThread extends Thread {

		private boolean mActive;
		private long mConnDelay;
		private boolean mReload;
		private boolean mRunning;

		private Socket mSocket;
		private InputStream mInputStream;
		private OutputStream mOutputStream;

		synchronized public boolean sendCommand(String command) {
			if (mOutputStream == null) {
				return false;
			}

			try {
				command += "\n";
				mOutputStream.write(command.getBytes());
				mOutputStream.flush();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}

			return false;
		}

		synchronized public void closeSocket() {
			if (mOutputStream != null) {
				try {
					mOutputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				mOutputStream = null;
			}

			if (mInputStream != null) {
				try {
					mInputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				mInputStream = null;
			}

			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				mSocket = null;
			}
		}

		synchronized public void setActive(boolean active) {
			if (active) {
				mActive = true;
				mConnDelay = 0;
			} else {
				mActive = false;
				closeSocket();
			}
		}

		synchronized private boolean isRunEnabled() {
			return mActive && MainActivity.isWanShareEnabled(getApplicationContext());
		}

		synchronized public void setConnDelay(long delay) {
			mConnDelay = delay;
		}

		synchronized public void reload() {
			mReload = true;
			setActive(true);
			closeSocket();
			notify();
		}

		synchronized boolean isRunning() {
			return mRunning;
		}

		@Override
		synchronized public void start() {
			setActive(true);
			super.start();
		}

		@Override
		public void run() {
			while (isRunEnabled()) {
				mReload = false;

				List<String> lines = MainActivity.getWanShareServer(getApplicationContext());
				if (lines == null || lines.isEmpty()) {
					break;
				}

				for (String line : lines) {
					if (!isRunEnabled()) {
						break;
					}

					try {
						int port;
						String host;

						String[] segs = line.split("\\s*:\\s*");
						if (segs.length > 1) {
							port = Integer.parseInt(segs[1].trim());
						} else {
							port = 8864;
						}

						host = segs[0].trim();
						if (host.isEmpty()) {
							continue;
						}

						CavanAndroid.dLog("host = " + host + ", port = " + port);

						String summary = host + ':' + port;

						mHandler.obtainMessage(MSG_TCP_SERVICE_STATE_CHANGED, R.string.wan_connecting, 0, summary).sendToTarget();

						mSocket = new Socket();
						mSocket.connect(new InetSocketAddress(host, port), 6000);

						mInputStream = mSocket.getInputStream();
						mOutputStream = mSocket.getOutputStream();

						Message message = mHandler.obtainMessage(MSG_TCP_SERVICE_STATE_CHANGED, R.string.wan_connected, 0, summary);
						mHandler.sendMessageDelayed(message, 1000);

						BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));

						mRunning = true;
						mHandler.sendEmptyMessage(MSG_KEEP_ALIVE);

						while (true) {
							try {
								line = reader.readLine();
							} catch (IOException e) {
								e.printStackTrace();
								line = null;
							}

							if (line == null) {
								break;
							}

							try {
								if (NET_CMD_KEEP_ALIVE.equals(line)) {
									CavanAndroid.dLog("Received: " + line);
								} else if (MainActivity.isWanReceiveEnabled(getApplicationContext())) {
									onNetworkCommandReceived("外网分享", line);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						mRunning = false;
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						closeSocket();
					}

					CavanAndroid.dLog("mReload = " + mReload);

					if (mReload) {
						break;
					}
				}

				mHandler.removeMessages(MSG_TCP_SERVICE_STATE_CHANGED);
				mHandler.obtainMessage(MSG_TCP_SERVICE_STATE_CHANGED, R.string.wan_disconnected, 0).sendToTarget();

				if (mActive) {
					synchronized (this) {
						try {
							if (mConnDelay < 30000) {
								mConnDelay = mConnDelay * 2 + 500;
							}

							CavanAndroid.dLog("mConnDelay = " + mConnDelay);
							wait(mConnDelay);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}

			mTcpDaemon = null;
		}
	}

	public class TcpBridgeThread extends Thread {

		private boolean mActive;

		public void killCommand() {
			CavanJni.kill("tcp_bridge");
		}

		public void setActive(boolean active) {
			if (active) {
				mActive = true;
			} else {
				mActive = false;
				killCommand();
			}
		}

		@Override
		public void run() {
			mActive = true;

			while (mActive && MainActivity.isTcpBridgeEnabled(getApplicationContext())) {
				String setting = MainActivity.getTcpBridgeSetting(getApplicationContext());
				if (setting == null) {
					break;
				}

				String[] settings = setting.split("\\n");
				if (settings.length != 2) {
					break;
				}

				String url1 = "tcp://" + CavanString.deleteSpace(settings[0]);
				String url2 = "tcp://" + CavanString.deleteSpace(settings[1]);

				mHandler.obtainMessage(MSG_TCP_BRIDGE_STATE_CHANGED, R.string.tcp_bridge_running, 0).sendToTarget();
				CavanJni.doTcpBridge(url1, url2);
				mHandler.obtainMessage(MSG_TCP_BRIDGE_STATE_CHANGED, R.string.tcp_bridge_exit, 0).sendToTarget();

				if (mActive) {
					synchronized (this) {
						try {
							wait(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

			mTcpBridge = null;
			mHandler.obtainMessage(MSG_TCP_BRIDGE_STATE_CHANGED, R.string.tcp_bridge_stopped, 0).sendToTarget();
		}
	}
}
