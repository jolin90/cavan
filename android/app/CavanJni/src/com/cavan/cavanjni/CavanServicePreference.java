package com.cavan.cavanjni;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.cavan.android.CavanAndroid;

@SuppressLint("HandlerLeak")
public abstract class CavanServicePreference extends EditTextPreference {

	private static final int EVENT_START_SERVICE = 1;
	private static final int EVENT_STOP_SERVICE = 2;
	private static final int EVENT_RESTART_SERVICE = 3;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			CavanAndroid.dLog("message = " + msg);

			switch (msg.what) {
			case EVENT_START_SERVICE:
				startService();
				break;

			case EVENT_STOP_SERVICE:
				stopService();
				break;

			case EVENT_RESTART_SERVICE:
				restartService();
				break;
			}

			super.handleMessage(msg);
		}
	};

	private boolean mNeedStop;
	private ICavanService mService;
	private boolean mReceiverRegisted;
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			CavanAndroid.dLog("onServiceDisconnected: " + arg0);

			mService = null;

			updateSummary(CavanService.STATE_STOPPED);
			setEnabled(false);

			mHandler.sendEmptyMessageDelayed(EVENT_START_SERVICE, 500);
		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			CavanAndroid.dLog("onServiceConnected: " + arg0);

			mService = ICavanService.Stub.asInterface(arg1);

			if (!mReceiverRegisted) {
				try {
					IntentFilter filter = new IntentFilter(mService.getAction());
					getContext().registerReceiver(mReceiver, filter);
					mReceiverRegisted = true;
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			updateSummary(getServiceState());
			setEnabled(true);
		}
	};

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateSummary(context, intent);
		}
	};

	public abstract Intent getServiceIntent(Context context);

	public CavanServicePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		updateSummary(CavanService.STATE_STOPPED);
		startService(context);
	}

	public void unbindService(Context context) {
		context.unbindService(mConnection);

		if (mReceiverRegisted) {
			try {
				context.unregisterReceiver(mReceiver);
				mReceiverRegisted = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startService(Context context) {
		Intent service = getServiceIntent(context);
		context.startService(service);
		context.bindService(service, mConnection, 0);
	}

	public void startService() {
		startService(getContext());
	}

	public void stopService(Context context) {
		context.stopService(getServiceIntent(context));
	}

	public void stopService() {
		stopService(getContext());
	}

	public void restartService() {
		stopService();
		mHandler.sendEmptyMessageDelayed(EVENT_START_SERVICE, 500);
	}

	public int getServiceState() {
		if (mService != null) {
			try {
				return mService.getState();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		return CavanService.STATE_STOPPED;
	}

	public boolean isServiceEnabled() {
		if (mService != null) {
			try {
				return mService.isEnabled();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public void start(int port) {
		if (mService != null) {
			try {
				mService.start(port);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean stop() {
		if (mService != null) {
			try {
				return mService.stop();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public int getPort() {
		if (mService != null) {
			try {
				return mService.getPort();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		return 0;
	}

	public void updateSummary(int state) {

		if (mService == null) {
			setSummary(R.string.service_disconnected);
		} else {
			Resources resources = getContext().getResources();
			if (resources == null) {
				setSummary(R.string.service_connected);
			} else {
				StringBuilder builder = new StringBuilder();

				int resId;

				switch (state) {
				case CavanService.STATE_PREPARE:
					resId = R.string.prepare;
					break;

				case CavanService.STATE_RUNNING:
					resId = R.string.running;
					break;

				case CavanService.STATE_WAITING:
					resId = R.string.waiting;
					break;

				default:
					resId = R.string.stopped;
				}

				builder.append(resources.getString(resId));
				builder.append(", ");
				builder.append(resources.getString(R.string.port));
				builder.append(": ");
				builder.append(Integer.toString(getPort()));

				setSummary(builder.toString());
			}
		}
	}

	public void updateSummary(Context context, Intent intent) {
		int state = intent.getIntExtra("state", CavanService.STATE_STOPPED);
		updateSummary(state);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			if (mNeedStop) {
				stop();
			} else {
				start(Integer.parseInt(getText()));
			}
		}
	}

	@Override
	protected void showDialog(Bundle state) {
		mNeedStop = isServiceEnabled();
		setText(Integer.toString(getPort()));

		super.showDialog(state);
	}

	@Override
	protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
		if (mNeedStop) {
			editText.setEnabled(false);
		} else {
			editText.setEnabled(true);
			editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		}

		super.onAddEditTextToDialogView(dialogView, editText);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		builder.setMessage(R.string.port);

		if (mNeedStop) {
			builder.setPositiveButton(R.string.stop, this);
		} else {
			builder.setPositiveButton(R.string.start, this);
		}

		super.onPrepareDialogBuilder(builder);
	}
}
