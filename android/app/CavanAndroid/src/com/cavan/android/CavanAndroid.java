package com.cavan.android;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cavan.java.CavanJava;

@SuppressWarnings("deprecation")
public class CavanAndroid {

	public static final int FLAG_NEEDS_MENU_KEY = 0x40000000;

	public static String TAG = "Cavan";
	public static boolean DLOG_ENABLE = true;
	public static boolean WLOG_ENABLE = true;
	public static boolean ELOG_ENABLE = true;
	public static boolean PLOG_ENABLE = true;

	public static final String CLIP_LABEL_DEFAULT = "Cavan";
	public static final String CLIP_LABEL_TEMP = CLIP_LABEL_DEFAULT + "Temp";

	public static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

	public static final int EVENT_CLEAR_TOAST = 1;

	private static HashMap<String, Object> mSystemServiceMap = new HashMap<String, Object>();

	private static Toast sToast;
	private static final Object sToastLock = new Object();

	private static Context sContext;
	private static Handler sHandler;
	private static CavanThreadedHandler sThreadedHandler;
	private static CavanWakeLock sWakeLock = new CavanWakeLock(false);
	private static CavanWakeLock sWakeupLock = new CavanWakeLock(true);
	private static CavanKeyguardLock sKeyguardLock = new CavanKeyguardLock();
	private static CavanMulticastLock sMulticastLock = new CavanMulticastLock();

	public static void eLog(String message) {
		if (ELOG_ENABLE) {
			Log.e(TAG, message);
		}
	}

	public static void eLog(String message, Throwable throwable) {
		if (ELOG_ENABLE) {
			Log.e(TAG, message, throwable);
		}
	}

	public static void wLog(String message) {
		if (WLOG_ENABLE) {
			Log.w(TAG, message);
		}
	}

	public static void wLog(Throwable throwable) {
		if (WLOG_ENABLE) {
			Log.w(TAG, throwable);
		}
	}

	public static void wLog(String message, Throwable throwable) {
		if (WLOG_ENABLE) {
			Log.w(TAG, message, throwable);
		}
	}

	public static void dLog(String message) {
		if (DLOG_ENABLE) {
			Log.d(TAG, message);
		}
	}

	public static void dLogLarge(String message) {
		ByteArrayInputStream stream = new ByteArrayInputStream(message.getBytes());
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

		while (true) {
			try {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				dLog(line);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}

		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void dLog(String message, Throwable throwable) {
		if (DLOG_ENABLE) {
			Log.d(TAG, message, throwable);
		}
	}

	public static void pLog() {
		if (PLOG_ENABLE) {
			eLog(CavanJava.buildPosMessage());
		}
	}

	public static void pLog(String message) {
		if (PLOG_ENABLE) {
			eLog(CavanJava.buildPosMessage(message));
		}
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

	public static void putSystemServiceCache(String name, Object service) {
		mSystemServiceMap.put(name, service);
	}

	public static Object getSystemServiceCached(Context context, String name) {
		Object service = mSystemServiceMap.get(name);
		if (service != null) {
			return service;
		}

		if (context != null) {
			sContext = context;
		} else if (sContext != null) {
			context = sContext;
		} else {
			return null;
		}

		service = context.getSystemService(name);
		if (service == null) {
			return null;
		}

		putSystemServiceCache(name, service);

		return service;
	}

	public static void cancelToast() {
		synchronized (sToastLock) {
			if (sToast != null) {
				sToast.cancel();
				sToast = null;
			}
		}
	}

	public static void showToast(Context context, String text, int duration) {
		dLog(text);

		synchronized (sToastLock) {
			if (sToast == null) {
				sToast = Toast.makeText(context, text, duration);
				sToast.setGravity(Gravity.CENTER, 0, 0);
			} else {
				sToast.setDuration(duration);
				sToast.setText(text);
			}

			sToast.show();
		}
	}

	public static void showToast(Context context, String text) {
		showToast(context, text, Toast.LENGTH_SHORT);
	}

	public static void showToastLong(Context context, String text) {
		showToast(context, text, Toast.LENGTH_LONG);
	}

	public static void showToast(Context context, int duration, int resId, Object... formatArgs) {
		String text = context.getResources().getString(resId, formatArgs);
		if (text != null) {
			showToast(context, text, duration);
		}
	}

	public static void showToast(Context context, int resId, Object... formatArgs) {
		showToast(context, Toast.LENGTH_SHORT, resId, formatArgs);
	}

	public static void showToastLong(Context context, int resId, Object... formatArgs) {
		showToast(context, Toast.LENGTH_LONG, resId, formatArgs);
	}

	public static boolean isMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	public static boolean isSubThread() {
		return Looper.myLooper() != Looper.getMainLooper();
	}

	public static boolean setLockScreenEnable(KeyguardManager manager, boolean enable) {
		if (enable) {
			sKeyguardLock.release();
		} else {
			return sKeyguardLock.acquire(manager);
		}

		return true;
	}

	public static boolean setLockScreenEnable(Context context, boolean enable) {
		if (enable) {
			sKeyguardLock.release();
		} else {
			return sKeyguardLock.acquire(context);
		}

		return true;
	}

	public static void releaseWakeLock() {
		CavanAndroid.dLog("releaseWakeLock");

		sWakeLock.release();
		sWakeupLock.release();
	}

	public static boolean acquireWakeLock(PowerManager manager, long overtime) {
		CavanAndroid.dLog("acquireWakeLock: overtime = " + overtime);
		return sWakeLock.acquire(manager, overtime);
	}

	public static boolean acquireWakeLock(PowerManager manager) {
		return acquireWakeLock(manager, 0);
	}

	public static boolean acquireWakeLock(Context context, long overtime) {
		PowerManager manager = (PowerManager) getSystemServiceCached(context, Context.POWER_SERVICE);
		if (manager == null) {
			return false;
		}

		return acquireWakeLock(manager, overtime);
	}

	public static boolean acquireWakeLock(Context context) {
		return acquireWakeLock(context, 0);
	}

	public static boolean acquireWakeupLock(PowerManager manager, long overtime) {
		CavanAndroid.dLog("acquireWakeupLock: overtime = " + overtime);
		return sWakeupLock.acquire(manager, overtime);
	}

	public static boolean acquireWakeupLock(PowerManager manager) {
		return acquireWakeupLock(manager, 0);
	}

	public static boolean acquireWakeupLock(Context context, long overtime) {
		PowerManager manager = (PowerManager) getSystemServiceCached(context, Context.POWER_SERVICE);
		if (manager == null) {
			return false;
		}

		return acquireWakeupLock(manager, overtime);
	}

	public static boolean acquireWakeupLock(Context context) {
		return acquireWakeupLock(context, 0);
	}

	public static boolean postClipboardText(ClipboardManager manager, CharSequence label, CharSequence text) {
		if (manager != null && text != null && text.length() > 0) {
			manager.setPrimaryClip(ClipData.newPlainText(label, text));
			return true;
		}

		return false;
	}

	public static boolean postClipboardText(ClipboardManager manager, CharSequence text) {
		return postClipboardText(manager, CLIP_LABEL_DEFAULT, text);
	}

	public static boolean postClipboardTextTemp(ClipboardManager manager, CharSequence text) {
		return postClipboardText(manager, CLIP_LABEL_TEMP, text);
	}

	public static ClipboardManager getClipboardManager(Context context) {
		return (ClipboardManager) getSystemServiceCached(context, Context.CLIPBOARD_SERVICE);
	}

	public static boolean postClipboardText(Context context, CharSequence label, CharSequence text) {
		return postClipboardText(getClipboardManager(context), label, text);
	}

	public static boolean postClipboardText(Context context, CharSequence text) {
		return postClipboardText(context, CLIP_LABEL_DEFAULT, text);
	}

	public static boolean postClipboardTextTemp(Context context, CharSequence text) {
		return postClipboardText(context, CLIP_LABEL_TEMP, text);
	}

	public static String getClipboardLabel(ClipData clip) {
		ClipDescription desc = clip.getDescription();
		if (desc == null) {
			return null;
		}

		CharSequence label = desc.getLabel();
		if (label == null) {
			return null;
		}

		return label.toString();
	}

	public static String getClipboardText(ClipboardManager manager) {
		if (manager == null) {
			return null;
		}

		CharSequence sequence = manager.getText();
		if (sequence == null) {
			return null;
		}

		return sequence.toString();
	}

	public static String getClipboardText(Context context) {
		return getClipboardText(getClipboardManager(context));
	}

	public static boolean sendNotification(Context context, int id, Notification notification) {
		NotificationManager manager = (NotificationManager) getSystemServiceCached(context, Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			return false;
		}

		manager.notify(id, notification);

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

	public static ActivityManager getActivityManager(Context context) {
		return (ActivityManager) getSystemServiceCached(context, Context.ACTIVITY_SERVICE);
	}

	public static ComponentName getTopActivityInfo(Context context) {
		ActivityManager manager = getActivityManager(context);
		if (manager == null) {
			return null;
		}

		return manager.getRunningTasks(1).get(0).topActivity;
	}

	public static RunningAppProcessInfo getTopAppProcessInfo(Context context) {
		ActivityManager manager = getActivityManager(context);
		if (manager == null) {
			return null;
		}

		List<RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
		if (infos == null) {
			return null;
		}

		Field field;

		try {
			field = RunningAppProcessInfo.class.getDeclaredField("processState");
		} catch (Exception e) {
			return null;
		}

		for (RunningAppProcessInfo info : infos) {
			if (info == null || info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				continue;
			}

			try {
				Integer state = field.getInt(info);
				if (state != null && state == 2) {
					return info;
				}
			} catch (Exception e) {
				return null;
			}
		}

		return null;
	}

	public static String getTopActivieyPackageName(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			ComponentName info = getTopActivityInfo(context);
			if (info != null) {
				return info.getPackageName();
			}
		} else {
			RunningAppProcessInfo info = getTopAppProcessInfo(context);
			if (info != null && info.pkgList.length > 0) {
				return info.pkgList[0];
			}
		}

		return null;
	}

	public static String getTopActivityClassName(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			ComponentName info = getTopActivityInfo(context);
			if (info != null) {
				return info.getClassName();
			}
		} else {
			RunningAppProcessInfo info = getTopAppProcessInfo(context);
			if (info != null) {
				return info.processName;
			}
		}

		return null;
	}

	public static boolean isTopActivity(Context context, String pkgName) {
		return pkgName.equals(getTopActivieyPackageName(context));
	}

	public static boolean setMulticastEnabled(Context context, boolean enable) {
		if (enable) {
			return sMulticastLock.acquire(context);
		}

		sMulticastLock.release();

		return true;
	}

	public static boolean showInputMethodPicker(Context context) {
		InputMethodManager manager = (InputMethodManager) getSystemServiceCached(context, Context.INPUT_METHOD_SERVICE);
		if (manager == null) {
			return false;
		}

		manager.showInputMethodPicker();

		return true;
	}

	public static boolean setMenuKeyVisibility(LayoutParams params, boolean visibility) {
		try {
			Field field = LayoutParams.class.getField("needsMenuKey");
			field.setInt(params, visibility ? 1 : 2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static boolean setMenuKeyVisibility(Window window, boolean visibility) {
		LayoutParams params = window.getAttributes();

		if (params != null && setMenuKeyVisibility(params, visibility)) {
			window.setAttributes(params);
			return true;
		}

		window.setFlags(FLAG_NEEDS_MENU_KEY, FLAG_NEEDS_MENU_KEY);

		return false;
	}

	public static boolean inKeyguardRestrictedInputMode(Context context) {
		KeyguardManager manager = (KeyguardManager) getSystemServiceCached(context, Context.KEYGUARD_SERVICE);
		if (manager == null) {
			return false;
		}

		return manager.inKeyguardRestrictedInputMode();
	}

	public static NetworkInfo getActiveNetworkInfo(Context context) {
		ConnectivityManager manager = (ConnectivityManager) getSystemServiceCached(context, Context.CONNECTIVITY_SERVICE);
		if (manager == null) {
			return null;
		}

		return manager.getActiveNetworkInfo();
	}

	public static boolean isNetworkAvailable(Context context) {
		NetworkInfo info = getActiveNetworkInfo(context);
		if (info == null) {
			return false;
		}

		return info.isAvailable();
	}

	public static void setWindowKeyguardEnable(Window window, boolean enable) {
		int flags = LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED;

		if (enable) {
			window.clearFlags(flags);
		} else {
			window.addFlags(flags);
		}
	}

	public static void setActivityKeyguardEnable(Activity activity, boolean enable) {
		setWindowKeyguardEnable(activity.getWindow(), enable);
	}

	public static boolean startActivity(Context context, Intent intent) {
		try {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			context.startActivity(intent);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static boolean startActivity(Context context, String pkgName) {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
		if (intent == null) {
			return false;
		}

		return startActivity(context, intent);
	}

	public static boolean startActivity(Context context, String pkgName, String clsName) {
		Intent intent = new Intent();
		intent.setClassName(pkgName, clsName);
		return CavanAndroid.startActivity(context, intent);
	}

	public static boolean startActivity(Context context, Class<?> cls) {
		return startActivity(context, new Intent(context, cls));
	}

	public static File getExternalStorageFile(String name) {
		return new File(Environment.getExternalStorageDirectory(), name);
	}

	public static String[] getVolumePaths(StorageManager manager) {
		Object object = CavanJava.invokeMethod(manager, "getVolumePaths");
		if (object != null) {
			return (String[]) object;
		}

		return new String[] { Environment.getExternalStorageDirectory().getPath() };
	}

	public static String[] getSupportedAbis() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			if (Build.CPU_ABI2.isEmpty()) {
				return new String[] { Build.CPU_ABI };
			} else {
				return new String[] { Build.CPU_ABI, Build.CPU_ABI2 };
			}
		}

		return Build.SUPPORTED_ABIS;
	}

	public static boolean setSoftInputEnable(Context context, View view, boolean enable) {
		InputMethodManager manager = (InputMethodManager) getSystemServiceCached(context, Context.INPUT_METHOD_SERVICE);
		if (manager == null) {
			return false;
		}

		if (enable) {
			return manager.showSoftInput(view, 0);
		} else {
			return manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	public static Handler getHandler() {
		if (sHandler == null) {
			try {
				sHandler = new Handler();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return sHandler;
	}

	public static CavanThreadedHandler getThreadedHandler() {
		if (sThreadedHandler == null) {
			try {
				sThreadedHandler = new CavanThreadedHandler(CavanAndroid.class);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return sThreadedHandler;
	}

	public static boolean postRunnable(Handler handler, Runnable runnable) {
		if (handler == null) {
			return false;
		}

		handler.removeCallbacks(runnable);
		handler.post(runnable);

		return true;
	}

	public static boolean postRunnable(Handler handler, Runnable runnable, long delayMillis) {
		if (handler == null) {
			return false;
		}

		handler.removeCallbacks(runnable);
		handler.postDelayed(runnable, delayMillis);

		return true;
	}

	public static boolean removeRunnable(Handler handler, Runnable runnable) {
		if (handler == null) {
			return false;
		}

		handler.removeCallbacks(runnable);

		return true;
	}

	public static boolean postRunnable(Runnable runnable) {
		return postRunnable(getHandler(), runnable);
	}

	public static boolean postRunnable(Runnable runnable, long delayMillis) {
		return postRunnable(getHandler(), runnable, delayMillis);
	}

	public static boolean removeRunnable(Runnable runnable) {
		return removeRunnable(getHandler(), runnable);
	}

	public static boolean postRunnableThreaded(Runnable runnable) {
		return postRunnable(getThreadedHandler(), runnable);
	}

	public static boolean postRunnableThreaded(Runnable runnable, long delayMillis) {
		return postRunnable(getThreadedHandler(), runnable, delayMillis);
	}

	public static boolean removeRunnableThreaded(Runnable runnable) {
		return removeRunnable(getThreadedHandler(), runnable);
	}

	public static boolean checkPermissions(int[] grantResults) {
		for (int grantResult : grantResults) {
			if (grantResult != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkPermissions(String[] permissions, int[] grantResults, String permission) {
		for (int i = permissions.length - 1; i >= 0; i--) {
			if (permission.equals(permissions[i])) {
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
					return true;
				} else {
					return false;
				}
			}
		}

		return false;
	}

	public static boolean checkPermissions(Context context, String[] permissions) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}

		for (String permission : permissions) {
			if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkAndRequestPermissions(Activity activity, String[] permissions, int requestCode) {
		if (checkPermissions(activity, permissions)) {
			return true;
		}

		activity.requestPermissions(permissions, requestCode);

		return false;
	}

	public static boolean checkAndRequestPermissions(Activity activity, String[] permissions) {
		return checkAndRequestPermissions(activity, permissions, 0);
	}

	public static boolean checkAndRequestPermission(Activity activity, String permission, int requestCode) {
		return checkAndRequestPermissions(activity, new String[] { permission }, requestCode);
	}

	public static boolean checkAndRequestPermission(Activity activity, String permission) {
		return checkAndRequestPermission(activity, permission, 0);
	}
}