package com.cavan.jwaootoy;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cavan.android.CavanAndroid;
import com.cavan.java.CavanProgressListener;
import com.cavan.java.CavanString;
import com.cavan.resource.JwaooToyActivity;
import com.jwaoo.android.JwaooBleToy;
import com.jwaoo.android.JwaooBleToy.JwaooToyAppSettings;
import com.jwaoo.android.JwaooBleToy.JwaooToyKeySettings;

@SuppressLint("HandlerLeak")
public class MainActivity extends JwaooToyActivity implements OnClickListener, OnCheckedChangeListener {

	public static final int SENSOR_DELAY = 20;

	private static final int EVENT_INIT_COMPLETE = 1;
	private static final int EVENT_OTA_START = 3;
	private static final int EVENT_OTA_FAILED = 4;
	private static final int EVENT_OTA_SUCCESS = 5;
	private static final int EVENT_PROGRESS_UPDATED = 6;
	private static final int EVENT_BATTERY_INFO = 11;
	private static final int EVENT_MOTO_RAND = 12;
	private static final int EVENT_UPDATE_SPEED = 13;

	private boolean mOtaBusy;
	private int mDataCount;

	private Button mButtonSend;
	private Button mButtonUpgrade;
	private Button mButtonReboot;
	private Button mButtonShutdown;
	private Button mButtonDisconnect;
	private Button mButtonReadBdAddr;
	private Button mButtonWriteBdAddr;

	private CheckBox mCheckBoxSensor;
	private CheckBox mCheckBoxClick;
	private CheckBox mCheckBoxLongClick;
	private CheckBox mCheckBoxMultiClick;
	private CheckBox mCheckBoxBatteryEvent;
	private CheckBox mCheckBoxFactoryMode;
	private CheckBox mCheckBoxMotoEvent;
	private CheckBox mCheckBoxKeyLock;
	private CheckBox mCheckBoxMotoRand;

	private ProgressBar mProgressBar;
	private EditText mEditTextBdAddr;
	private Spinner mSpinnerMotoMode;
	private Spinner mSpinnerMotoLevel;
	private TextView mTextViewBatteryInfo;

	private Button mButtonSuspendOvertime;
	private EditText mEditTextSuspendOvertime;

	private JwaooToyAppSettings mAppSettings;
	private JwaooToyKeySettings mKeySettings;

	private int mMotoMode;
	private int mMotoLevel;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_INIT_COMPLETE:
				if (mAppSettings != null) {
					mEditTextSuspendOvertime.setText(Integer.toString(mAppSettings.getSuspendDelay()));
				}
				break;

			case EVENT_OTA_START:
				updateUI(false);
				CavanAndroid.showToast(getApplicationContext(), R.string.text_upgrade_start);
				break;

			case EVENT_OTA_FAILED:
				mButtonUpgrade.setEnabled(true);
				mButtonSend.setEnabled(true);
				CavanAndroid.showToast(getApplicationContext(), R.string.text_upgrade_failed);
				break;

			case EVENT_OTA_SUCCESS:
				updateUI(true);
				CavanAndroid.showToast(getApplicationContext(), R.string.text_upgrade_successfull);
				break;

			case EVENT_PROGRESS_UPDATED:
				mProgressBar.setProgress(msg.arg1);
				break;

			case EVENT_BATTERY_INFO:
				mTextViewBatteryInfo.setText((CharSequence) msg.obj);
				break;

			case EVENT_MOTO_RAND:
				removeMessages(EVENT_MOTO_RAND);

				if (mCheckBoxMotoRand.isChecked() && mBleToy != null) {
					while (true) {
						int mode = (int) (Math.random() * 100 % 6 + 1);
						if (mode != mMotoMode) {
							mMotoMode = mode;
							break;
						}
					}

					CavanAndroid.dLog("mode = " + mMotoMode);

					if (mBleToy.setMotoMode(mMotoMode, JwaooBleToy.MOTO_LEVEL_MAX)) {
						sendEmptyMessageDelayed(EVENT_MOTO_RAND, 5000);
					}
				}
				break;

			case EVENT_UPDATE_SPEED:
				sendEmptyMessageDelayed(EVENT_UPDATE_SPEED, 1000);

				int count = mDataCount;

				if (count > 0) {
					mDataCount = 0;
					setTitle(String.format("count = %d, delay = %f", count, 1000.0 / count));
				}
				break;
			}
		}
	};

	@Override
	protected void onCreateBle(Bundle savedInstanceState) {
		setContentView(R.layout.activity_main);

		mButtonSend = (Button) findViewById(R.id.buttonSend);
		mButtonSend.setOnClickListener(this);

		mButtonUpgrade = (Button) findViewById(R.id.buttonUpgrade);
		mButtonUpgrade.setOnClickListener(this);

		mButtonReboot = (Button) findViewById(R.id.buttonReboot);
		mButtonReboot.setOnClickListener(this);

		mButtonShutdown = (Button) findViewById(R.id.buttonShutdown);
		mButtonShutdown.setOnClickListener(this);

		mButtonDisconnect = (Button) findViewById(R.id.buttonDisconnect);
		mButtonDisconnect.setOnClickListener(this);

		mButtonReadBdAddr = (Button) findViewById(R.id.buttonReadBdAddr);
		mButtonReadBdAddr.setOnClickListener(this);

		mButtonWriteBdAddr = (Button) findViewById(R.id.buttonWriteBdAddr);
		mButtonWriteBdAddr.setOnClickListener(this);

		mCheckBoxSensor = (CheckBox) findViewById(R.id.checkBoxSensor);
		mCheckBoxSensor.setOnCheckedChangeListener(this);

		mCheckBoxClick = (CheckBox) findViewById(R.id.checkBoxClick);
		mCheckBoxClick.setOnCheckedChangeListener(this);

		mCheckBoxLongClick = (CheckBox) findViewById(R.id.checkBoxLongClick);
		mCheckBoxLongClick.setOnCheckedChangeListener(this);

		mCheckBoxMultiClick = (CheckBox) findViewById(R.id.checkBoxMultiClick);
		mCheckBoxMultiClick.setOnCheckedChangeListener(this);

		mCheckBoxBatteryEvent = (CheckBox) findViewById(R.id.checkBoxBattEvent);
		mCheckBoxBatteryEvent.setOnCheckedChangeListener(this);

		mCheckBoxMotoEvent = (CheckBox) findViewById(R.id.checkBoxMotoEvent);
		mCheckBoxMotoEvent.setOnCheckedChangeListener(this);

		mCheckBoxMotoRand = (CheckBox) findViewById(R.id.checkBoxMotoRand);
		mCheckBoxMotoRand.setOnCheckedChangeListener(this);

		mCheckBoxFactoryMode = (CheckBox) findViewById(R.id.checkBoxFactoryMode);
		mCheckBoxFactoryMode.setOnCheckedChangeListener(this);

		mCheckBoxKeyLock = (CheckBox) findViewById(R.id.checkBoxKeyLock);
		mCheckBoxKeyLock.setOnCheckedChangeListener(this);

		mProgressBar = (ProgressBar) findViewById(R.id.progressBarUpgrade);
		mEditTextBdAddr = (EditText) findViewById(R.id.editTextBdAddr);
		mTextViewBatteryInfo = (TextView) findViewById(R.id.textViewBatteryInfo);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.text_moto_modes, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerMotoMode = (Spinner) findViewById(R.id.spinnerMotoMode);
		mSpinnerMotoMode.setAdapter(adapter);
		mSpinnerMotoMode.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mMotoMode = position;
				setMotoMode();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		List<CharSequence> list = new ArrayList<CharSequence>();
		String text = getResources().getString(R.string.text_moto_level);
		for (int i = 0; i <= 18; i++) {
			list.add(text + i);
		}

		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerMotoLevel = (Spinner) findViewById(R.id.spinnerMotoLevel);
		mSpinnerMotoLevel.setAdapter(adapter);
		mSpinnerMotoLevel.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mMotoLevel = position;
				setMotoMode();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		mEditTextSuspendOvertime = (EditText) findViewById(R.id.editTextSuspendOvertime);
		mButtonSuspendOvertime = (Button) findViewById(R.id.buttonSuspendOvertime);
		mButtonSuspendOvertime.setOnClickListener(this);

		showScanActivity();
		mHandler.sendEmptyMessage(EVENT_UPDATE_SPEED);
	}

	private void setUpgradeProgress(int progress) {
		mHandler.obtainMessage(EVENT_PROGRESS_UPDATED, progress, 0).sendToTarget();
	}

	private boolean setMotoMode() {
		if (mBleToy != null && mBleToy.isConnected()) {
			boolean success = mBleToy.setMotoMode(mMotoMode, mMotoLevel);

			if (success) {
				CavanAndroid.dLog("Moto: mode = " + mMotoMode + ", level = " + mMotoLevel);
			} else {
				CavanAndroid.dLog("Failed to setMotoMode");
				CavanAndroid.dumpstack();
			}

			return success;
		}

		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonSend:
			String identify = mBleToy.doIdentify();
			if (identify == null) {
				break;
			}

			CavanAndroid.dLog("identify = " + identify);

			String buildDate = mBleToy.readBuildDate();
			if (buildDate == null) {
				break;
			}

			CavanAndroid.dLog("buildDate = " + buildDate);

			int version = mBleToy.readVersion();
			if (version == 0) {
				break;
			}

			CavanAndroid.dLog("version = " + Integer.toHexString(version));
			break;

		case R.id.buttonUpgrade:
			if (mOtaBusy) {
				break;
			}

			mOtaBusy = true;

			new Thread() {

				@Override
				public void run() {
					mHandler.sendEmptyMessage(EVENT_OTA_START);
					if (mBleToy.doOtaUpgrade("/mnt/sdcard/jwaoo-toy.hex", new CavanProgressListener() {

						@Override
						public void onProgressUpdated(int progress) {
							setUpgradeProgress(progress);
						}
					})) {
						mHandler.sendEmptyMessage(EVENT_OTA_SUCCESS);
					} else {
						mHandler.sendEmptyMessage(EVENT_OTA_FAILED);
					}

					mOtaBusy = false;
				}
			}.start();
			break;

		case R.id.buttonReboot:
			mBleToy.doReboot();
			break;

		case R.id.buttonShutdown:
			mBleToy.doShutdown();
			break;

		case R.id.buttonDisconnect:
			if (mBleToy != null) {
				mBleToy.disconnect();
			}

			showScanActivity();
			break;

		case R.id.buttonReadBdAddr:
			String addr = mBleToy.readBdAddressString();
			if (addr != null) {
				mEditTextBdAddr.setText(addr);
			} else {
				CavanAndroid.dLog("Failed to readBdAddress");
			}
			break;

		case R.id.buttonWriteBdAddr:
			if (mBleToy.writeBdAddress(mEditTextBdAddr.getText().toString())) {
				CavanAndroid.dLog("writeBdAddress successfull");
			} else {
				CavanAndroid.dLog("Failed to writeBdAddress");
			}
			break;

		case R.id.buttonSuspendOvertime:
			if (mAppSettings == null) {
				mAppSettings = mBleToy.readAppSettings();
				if (mAppSettings == null) {
					mEditTextSuspendOvertime.setText(CavanString.EMPTY_STRING);
					break;
				}
			}

			try {
				int delay = Integer.valueOf(mEditTextSuspendOvertime.getText().toString());
				mAppSettings.setSuspendDelay(delay);
				if (mBleToy.writeAppSettings(mAppSettings)) {
					CavanAndroid.showToast(this, R.string.setting_success);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	@Override
	protected boolean onInitialize() {
		if (mBleToy.setClickEnable(mCheckBoxClick.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setClickEnable");
			return false;
		}

		if (mBleToy.setLongClickEnable(mCheckBoxLongClick.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setLongClickEnable");
			return false;
		}

		if (mBleToy.setMultiClickEnable(mCheckBoxMultiClick.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setMultiClickEnable");
			return false;
		}

		if (mBleToy.setBatteryEventEnable(mCheckBoxBatteryEvent.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setBatteryEventEnable");
			return false;
		}

		if (mBleToy.setFactoryModeEnable(mCheckBoxFactoryMode.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setFactoryModeEnable");
			return false;
		}

		if (mBleToy.setMotoEventEnable(mCheckBoxMotoEvent.isChecked()) == false && mBleToy.isCommandTimeout()) {
			CavanAndroid.dLog("Failed to setMotoEventEnable");
			return false;
		}

		mBleToy.setKeyLock(mCheckBoxKeyLock.isChecked());

		if (mMotoMode > 0 || mMotoLevel > 0) {
			if (setMotoMode() == false && mBleToy.isCommandTimeout()) {
				return false;
			}
		}

		mAppSettings = mBleToy.readAppSettings();
		CavanAndroid.dLog("JwaooToyAppSettings = " + mAppSettings);

		mKeySettings = mBleToy.readKeySettings();
		CavanAndroid.dLog("JwaooToyKeySettings = " + mKeySettings);
		CavanAndroid.dLog("JwaooToyBatteryInfo = " + mBleToy.getBatteryInfo());

		mBleToy.setKeyReportEnable(0x0f);

		if (!mBleToy.setSensorEnable(mCheckBoxSensor.isChecked(), SENSOR_DELAY)) {
			CavanAndroid.dLog("Failed to setSensorEnable");
			return false;
		}

		mHandler.sendEmptyMessage(EVENT_INIT_COMPLETE);

		return true;
	}

	@Override
	protected void onSensorDataReceived(byte[] data) {
		mDataCount++;
		CavanAndroid.dLog("onSensorDataReceived: " + mBleToy.getSensor().getAccelText());
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.checkBoxSensor:
			mBleToy.setSensorEnable(isChecked, SENSOR_DELAY);
			break;

		case R.id.checkBoxClick:
			mBleToy.setClickEnable(isChecked);
			break;

		case R.id.checkBoxLongClick:
			mBleToy.setLongClickEnable(isChecked);
			break;

		case R.id.checkBoxMultiClick:
			mBleToy.setMultiClickEnable(isChecked);
			break;

		case R.id.checkBoxBattEvent:
			mBleToy.setBatteryEventEnable(isChecked);
			break;

		case R.id.checkBoxFactoryMode:
			mBleToy.setFactoryModeEnable(isChecked);
			break;

		case R.id.checkBoxMotoEvent:
			mBleToy.setMotoEventEnable(isChecked);
			break;

		case R.id.checkBoxKeyLock:
			mBleToy.setKeyLock(isChecked);
			break;

		case R.id.checkBoxMotoRand:
			if (isChecked) {
				mHandler.sendEmptyMessage(EVENT_MOTO_RAND);
			} else if (mBleToy != null) {
				mBleToy.setMotoMode(0, 0);
			}
			break;
		}
	}

	@Override
	protected void onBatteryStateChanged(int state, int level, double voltage) {
		CavanAndroid.dLog("capacity = " + mBleToy.getBatteryCapacityByVoltage(voltage));

		StringBuilder builder = new StringBuilder();
		builder.append("level = ").append(level);
		builder.append(", voltage = ").append(voltage);
		builder.append(", state = ").append(JwaooBleToy.getBatteryStateString(state));
		mHandler.obtainMessage(EVENT_BATTERY_INFO, builder.toString()).sendToTarget();
	}
}
