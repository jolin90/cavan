package com.cavan.huahardwareinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.Xml;

public class HuaTouchscreenDevice {
	private static final String TAG = "Cavan";

	public static final int FW_STATE_UPGRADE_FAILED = -1;
	public static final int FW_STATE_UPGRADE_PREPARE = 0;
	public static final int FW_STATE_UPGRADE_STOPPING = 1;
	public static final int FW_STATE_UPGRADE_COMPLETE = 2;

	private static final String PROP_TP_FW_UPGRADE_PENDING = "persist.sys.tp.fw.pending";
	private static final String SETTING_TP_FW_UPGRADE_PENDING = "hua_tp_fw_pending";
	private static final String SETTING_TP_FW_UPGRADE_AUTO = "hua_tp_fw_upgrade_auto";

	private static final File mFileVersionXml = new File("/system/firmware/version.xml");

	private static final HuaTouchscreenDevice[] mTouchscreenList = {
		new HuaTouchscreenDevice("CY8C242", "/sys/bus/i2c/devices/i2c-2/2-0024/firmware_id", "cy8c242.iic"),
		new HuaTouchscreenDevice("FT6306", "/dev/FT5216", "/sys/bus/i2c/devices/i2c-2/2-0038/firmware_id", "FT6306.bin"),
		new HuaTouchscreenDevice("MSG21XX", "/sys/bus/i2c/devices/1-0026/firmware_id", "msg21xx.bin"),
		new HuaTouchscreenDevice("MSG21XX", "/sys/bus/i2c/devices/2-0026/firmware_id", "msg21xx.bin"),
	};

	private String mIcName;
	private String mFwName;
	private File mFileDevice;
	private File mFileFwId;
	private File mFileFw;
	int mMaxProgress;
	int mProgress;
	private HuaTouchscreenVendorInfo mVendorInfo;

	private Handler mHandler;

	public HuaTouchscreenDevice(String icName, String devPath, String fwIdPath, String fwName) {
		super();
		mIcName = icName;
		mFwName = fwName;
		mFileDevice = new File(devPath);
		mFileFwId = new File(fwIdPath);
	}

	public HuaTouchscreenDevice(String icName, String fwIdPath, String fwName) {
		this(icName, "/dev/HUA-" + icName, fwIdPath, fwName);
	}

	private String readFwId() {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(mFileFwId);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		byte[] buff;
		try {
			buff = new byte[inputStream.available()];
		} catch (IOException e1) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		try {
			int rdLen = inputStream.read(buff);
			return new String(buff, 0, rdLen);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public HuaTouchscreenVendorInfo readVendorInfo() {
		String fwIdContent = readFwId();
		if (fwIdContent == null) {
			return null;
		}

		int vendorId, fwId;

		Log.d(TAG, "fwIdContent = " + fwIdContent);
		String[] ids = fwIdContent.split("\\.");
		Log.d(TAG, "ids = " + ids + ", length = " + ids.length);
		if (ids.length > 1) {
			vendorId = Integer.parseInt(ids[0].trim());
			fwId = Integer.parseInt(ids[1].trim());
		} else if (ids.length > 0) {
			String idContent = ids[0].trim();
			int length = idContent.length() / 2;
			int base = length == 2 ? 16 : 10;
			vendorId = Integer.parseInt(idContent.substring(0, length), base);
			fwId = Integer.parseInt(idContent.substring(length), base);
		} else {
			return null;
		}

		return new HuaTouchscreenVendorInfo(vendorId, fwId);
	}

	public void fillVendorInfo() {
		mVendorInfo = readVendorInfo();
		if (mVendorInfo == null) {
			mVendorInfo = new HuaTouchscreenVendorInfo(-1, -1);
		}
	}

	public static HuaTouchscreenDevice getTouchscreenDevice() {
		for (HuaTouchscreenDevice device : mTouchscreenList) {
			if (device.mFileDevice.exists() && device.mFileFwId.exists()) {
				device.fillVendorInfo();
				return device;
			}
		}

		return null;
	}

	public int getFwVersionFromXml() throws XmlPullParserException, IOException, NumberFormatException {
		String fwName = getFwName();
		if (fwName == null) {
			return -1;
		}

		FileInputStream inputStream = new FileInputStream(mFileVersionXml );
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(inputStream, "UTF-8");

		int event;
		try {
			event = parser.getEventType();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return -1;
		}

		while (true) {
			switch (event) {
			case XmlPullParser.END_DOCUMENT:
				return -1;

			case XmlPullParser.START_TAG:
				if (parser.getName().equals("firmware") == false) {
					break;
				}

				String name = null;
				String version = null;

				for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
					if (parser.getAttributeName(i).equals("name")) {
						name = parser.getAttributeValue(i);
					} else if (parser.getAttributeName(i).equals("version")) {
						version = parser.getAttributeValue(i);
					}
				}

				Log.d(TAG, "name = " + name + ", version = " + version);

				if (name != null && version != null && name.equals(fwName)) {
					if (version.startsWith("0x") || version.startsWith("0X")) {
						version = version.substring(2);
					}

					return Integer.parseInt(version, 16);
				}
				break;
			}

			event = parser.nextTag();
		}
	}

	public boolean ifNeedAutoUpgrade() {
		if (mVendorInfo == null) {
			return false;
		}

		try {
			int version_new = getFwVersionFromXml();
			Log.d(TAG, "version new = " + version_new);
			int version_now = mVendorInfo.getFwVersion();
			Log.d(TAG, "version now = " + version_now);
			return version_new > version_now;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	public String getIcName() {
		return mIcName;
	}

	public File getFileDevice() {
		return mFileDevice;
	}

	public File getFileFwId() {
		return mFileFwId;
	}

	public String getFwName() {
		if (mVendorInfo == null) {
			return null;
		}

		String project;

		if (Build.DEVICE.equals("P810N30") || Build.DEVICE.equals("APT_TW_P810N30")) {
			project = "zc2501";
		} else if (Build.DEVICE.equals("P810E01")) {
			project = "zc2351";
		} else {
			project = Build.BOARD;
		}

		return project + "_" + mFwName + "_" + mVendorInfo.getShortName();
	}

	public HuaTouchscreenVendorInfo getVendorInfo() {
		return mVendorInfo;
	}

	private boolean fwUpgrade() {
		sendFwUpgradeState(FW_STATE_UPGRADE_PREPARE);

		FileInputStream inputStream;

		try {
			inputStream = new FileInputStream(mFileFw);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		FileOutputStream outputStream;

		try {
			outputStream = new FileOutputStream(mFileDevice);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			try {
				inputStream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return false;
		}

		boolean result = true;

		try {
			int available = inputStream.available();
			int wrTotal = 0;

			while (wrTotal < available) {
				byte[] buff = new byte[512];
				int rdLen = inputStream.read(buff);
				if (rdLen <= 0) {
					result = false;
				}

				outputStream.write(buff, 0, rdLen);
				wrTotal += rdLen;
				updateProgress(wrTotal * mMaxProgress / available);
			}

			sendFwUpgradeState(FW_STATE_UPGRADE_STOPPING);
		} catch (IOException e1) {
			e1.printStackTrace();
			result = false;
		}

		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}

		return result;
	}

	private void updateProgress(int progress) {
		if (mProgress != progress) {
			Log.d(TAG, "mProgress = " + mProgress);
			mProgress = progress;

			if (mHandler != null) {
				Message message = mHandler.obtainMessage(HuaTpUpgradeDialog.MSG_PROGRESS_CHANGED, mProgress, 0);
				message.sendToTarget();
			}
		}
	}

	private void sendFwUpgradeState(int state) {
		if (mHandler != null) {
			Message message = mHandler.obtainMessage(HuaTpUpgradeDialog.MSG_STATE_CHANGED, state, 0);
			message.sendToTarget();
		}
	}

	public void setFileFw(File fileFw) {
		mFileFw = fileFw;
	}

	public File getFileFw() {
		return mFileFw;
	}

	public void fwUpgrade(final Context context, int maxProgress, Handler handler) {
		mProgress = 0;
		mMaxProgress = maxProgress;
		mHandler = handler;

		Thread thread = new Thread() {
			public void run() {
				setPendingFirmware(context, mFileFw.getName());
				sendFwUpgradeState(fwUpgrade() ? FW_STATE_UPGRADE_COMPLETE : FW_STATE_UPGRADE_FAILED);
			}
		};

		thread.start();
	}

	public static String getPendingFirmware(final Context context) {
		String value = SystemProperties.get(PROP_TP_FW_UPGRADE_PENDING);
		if (value != null && value.length() > 0) {
			return value;
		}

		return Settings.System.getString(context.getContentResolver(), SETTING_TP_FW_UPGRADE_PENDING);
	}

	public static void setPendingFirmware(final Context context, String fwName) {
		Settings.System.putString(context.getContentResolver(), SETTING_TP_FW_UPGRADE_PENDING, fwName);
		SystemProperties.set(PROP_TP_FW_UPGRADE_PENDING, fwName);
	}

	public static boolean getAutoUpgrade(final Context context) {
		try {
			return Settings.System.getInt(context.getContentResolver(), SETTING_TP_FW_UPGRADE_AUTO) > 0;
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		return true;
	}

	public static void setAutoUpgrade(final Context context, boolean auto) {
		Settings.System.putInt(context.getContentResolver(), SETTING_TP_FW_UPGRADE_AUTO, auto ? 1 : 0);
	}
}
