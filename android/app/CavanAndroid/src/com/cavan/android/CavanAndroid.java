package com.cavan.android;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.input.InputManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cavan.java.CavanJava;

@SuppressWarnings("deprecation")
public class CavanAndroid {

	public static String TAG = "Cavan";

	public static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

	public static final int EVENT_CLEAR_TOAST = 1;

	private static Toast sToast;
	private static final Object sToastLock = new Object();

	private static WakeLock sWakeLock;
	private static PowerManager sPowerManager;

	private static KeyguardLock sKeyguardLock;
	private static KeyguardManager sKeyguardManager;

	private static ClipboardManager sClipboardManager;
	private static NotificationManager sNotificationManager;

	private static ActivityManager sActivityManager;
	public static InputMethodManager sInputMethodManager;

	private static MulticastLock sMulticastLock;
	private static WifiManager sWifiManager;

	public static void eLog(String message) {
		Log.e(TAG, message);
	}

	public static void eLog(String message, Throwable throwable) {
		Log.e(TAG, message, throwable);
	}

	public static void wLog(String message) {
		Log.w(TAG, message);
	}

	public static void wLog(Throwable throwable) {
		Log.w(TAG, throwable);
	}

	public static void wLog(String message, Throwable throwable) {
		Log.w(TAG, message, throwable);
	}

	public static void dLog(String message) {
		Log.d(TAG, message);
	}

	public static int dLog(String message, Throwable throwable) {
		return Log.d(TAG, message, throwable);
	}

	public static void pLog() {
		eLog(CavanJava.buildPosMessage());
	}

	public static void pLog(String message) {
		eLog(CavanJava.buildPosMessage(message));
	}

	public static void efLog(String format, Object... args) {
		eLog(String.format(format, args));
	}

	public static void wfLog(String format, Object... args) {
		wLog(String.format(format, args));
	}

	public static void dfLog(String format, Object... args) {
		dLog(String.format(format, args));
	}

	public static void pfLog(String format, Object... args) {
		pLog(String.format(format, args));
	}

	public static void dumpstack(Throwable throwable) {
		wLog(throwable);
	}

	public static void dumpstack() {
		wLog(new Throwable());
	}

	public static void cancelToastLocked() {
		if (sToast != null) {
			sToast.cancel();
			sToast = null;
		}
	}

	public static void cancelToast() {
		synchronized (sToastLock) {
			cancelToastLocked();
		}
	}

	public static void showToast(Context context, String text, int duration) {
		dLog(text);

		Toast toast = Toast.makeText(context, text, duration);
		synchronized (sToastLock) {
			cancelToastLocked();

			sToast = toast;
			toast.show();
		}
	}

	public static void showToast(Context context, String text) {
		showToast(context, text, Toast.LENGTH_SHORT);
	}

	public static void showToastLong(Context context, String text) {
		showToast(context, text, Toast.LENGTH_LONG);
	}

	public static void showToast(Context context, int resId, int duration) {
		String text = context.getResources().getString(resId);
		if (text != null) {
			showToast(context, text, duration);
		}
	}

	public static void showToast(Context context, int resId) {
		showToast(context, resId, Toast.LENGTH_SHORT);
	}

	public static void showToastLong(Context context, int resId) {
		showToast(context, resId, Toast.LENGTH_LONG);
	}

	public static boolean isMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	public static boolean isSubThread() {
		return Looper.myLooper() != Looper.getMainLooper();
	}

	public static boolean setLockScreenEnable(KeyguardManager manager, boolean enable) {
		if (enable) {
			if (sKeyguardLock != null) {
				sKeyguardLock.reenableKeyguard();
			}
		} else {
			if (sKeyguardLock == null) {
				sKeyguardLock = manager.newKeyguardLock(CavanAndroid.class.getName());
				if (sKeyguardLock == null) {
					return false;
				}
			}

			sKeyguardLock.disableKeyguard();
		}

		return true;
	}

	public static boolean setLockScreenEnable(Context context, boolean enable) {
		if (sKeyguardManager == null) {
			sKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
			if (sKeyguardManager == null) {
				return false;
			}
		}

		return setLockScreenEnable(sKeyguardManager, enable);
	}

	public static boolean setSuspendEnable(PowerManager manager, boolean enable, long timeout) {
		if (enable) {
			if (sWakeLock != null && sWakeLock.isHeld()) {
				sWakeLock.release();
			}
		} else {
			if (sWakeLock == null) {
				sWakeLock = manager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, CavanAndroid.class.getName());
				if (sWakeLock == null) {
					return false;
				}
			}

			if (timeout > 0) {
				sWakeLock.acquire(timeout);
			} else {
				sWakeLock.acquire();
			}
		}

		return true;
	}

	public static boolean setSuspendEnable(PowerManager manager, boolean enable) {
		return setSuspendEnable(manager, enable, 0);
	}

	public static boolean setSuspendEnable(Context context, boolean enable, long timeout) {
		if (sPowerManager == null) {
			sPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (sPowerManager == null) {
				return false;
			}
		}

		return setSuspendEnable(sPowerManager, enable, timeout);
	}

	public static boolean setSuspendEnable(Context context, boolean enable) {
		return setSuspendEnable(context, enable, 0);
	}

	public static void postClipboardText(ClipboardManager manager, CharSequence label, CharSequence text) {
		manager.setPrimaryClip(ClipData.newPlainText(label, text));
	}

	public static void postClipboardText(ClipboardManager manager, CharSequence text) {
		postClipboardText(manager, "cavan", text);
	}

	public static boolean postClipboardText(Context context, CharSequence label, CharSequence text) {
		if (sClipboardManager == null) {
			sClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			if (sClipboardManager == null) {
				return false;
			}
		}

		postClipboardText(sClipboardManager, label, text);

		return true;
	}

	public static boolean postClipboardText(Context context, CharSequence text) {
		return postClipboardText(context, "cavan", text);
	}

	public static boolean sendNotification(Context context, int id, Notification notification) {
		if (sNotificationManager == null) {
			sNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (sNotificationManager == null) {
				return false;
			}
		}

		sNotificationManager.notify(id, notification);

		return true;
	}

	public static String[] getEnabledAccessibilityServices(Context context) {
		String text = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
		if (text == null) {
			return new String[0];
		}

		return text.split(":");
	}

	public static boolean isAccessibilityServiceEnabled(Context context) {
		return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) > 0;
	}

	public static boolean isAccessibilityServiceEnabled(Context context, String service) {
		if (isAccessibilityServiceEnabled(context)) {
			for (String item : getEnabledAccessibilityServices(context)) {
				if (item.equals(service)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isAccessibilityServiceEnabled(Context context, Class<?> cls) {
		String service = context.getPackageName() + "/" + cls.getName();

		return isAccessibilityServiceEnabled(context, service);
	}

	public static String[] getEnabledNotificationListeners(Context context) {
		String text = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
		if (text == null) {
			return new String[0];
		}

		return text.split(":");
	}

	public static boolean isNotificationListenerEnabled(Context context, String service) {
		for (String listener : getEnabledNotificationListeners(context)) {
			if (listener.equals(service)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isNotificationListenerEnabled(Context context, Class<?> cls) {
		String service = context.getPackageName() + "/" + cls.getName();

		return isNotificationListenerEnabled(context, service);
	}

	public static ApplicationInfo getApplicationInfo(Context context, String packageName) {
		try {
			return context.getPackageManager().getApplicationInfo(packageName, 0);
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	public static CharSequence getApplicationLabel(Context context, String packageName) {
		PackageManager manager = context.getPackageManager();

		try {
			return manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0));
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	public static boolean isHuaweiPhone() {
		String id = SystemProperties.getClientIdBase();
		return (id != null && id.contains("huawei"));
	}

	public static String getDefaultInputMethod(Context context) {
		return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
	}

	public static String getPreference(Context context, String key, String defValue) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences == null) {
			return defValue;
		}

		return preferences.getString(key, defValue);
	}

	public static boolean putPreference(Context context, String key, String value) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences == null) {
			return false;
		}

		Editor editor = preferences.edit();

		editor.putString(key, value);

		return editor.commit();
	}

	public static boolean isPreferenceEnabled(Context context, String key) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences == null) {
			return false;
		}

		return preferences.getBoolean(key, false);
	}

	public static ComponentName getTopActivityInfo(Context context) {
		if (sActivityManager == null) {
			sActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			if (sActivityManager == null) {
				return null;
			}
		}

		return sActivityManager.getRunningTasks(1).get(0).topActivity;
	}

	public static boolean isTopActivity(Context context, String pkgName) {
		ComponentName info = getTopActivityInfo(context);
		return (info != null && pkgName.equals(info.getPackageName()));
	}

	public static WifiManager getWifiManager(Context context) {
		if (sWifiManager != null) {
			return sWifiManager;
		}

		sWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		return sWifiManager;
	}

	public static MulticastLock getMulticastLock(Context context) {
		if (sMulticastLock != null) {
			return sMulticastLock;
		}

		WifiManager manager = getWifiManager(context);
		if (manager == null) {
			return null;
		}

		sMulticastLock = manager.createMulticastLock(TAG);

		return sMulticastLock;
	}

	public static boolean setMulticastEnabled(Context context, boolean enable) {
		MulticastLock lock = getMulticastLock(context);
		if (lock == null) {
			return false;
		}

		if (enable) {
			lock.acquire();
		} else {
			lock.release();
		}

		return true;
	}

	public static InputMethodManager getInputMethodManager(Context context) {
		if (sInputMethodManager == null) {
			sInputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		}

		return sInputMethodManager;
	}

	public static boolean showInputMethodPicker(Context context) {
		InputMethodManager manager = getInputMethodManager(context);
		if (manager == null) {
			return false;
		}

		manager.showInputMethodPicker();

		return true;
	}
}
