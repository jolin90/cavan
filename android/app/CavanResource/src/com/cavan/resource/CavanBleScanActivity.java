package com.cavan.resource;

import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ListView;

import com.cavan.android.CavanAndroid;
import com.cavan.android.CavanBleDevice;
import com.cavan.android.CavanBleDeviceAdapter;
import com.cavan.android.CavanBleScanner;
import com.cavan.android.CavanQrCodeView;
import com.cavan.android.CavanQrCodeView.DecodeEventListener;
import com.google.zxing.Result;

@SuppressWarnings("deprecation")
public class CavanBleScanActivity extends CavanBleActivity implements OnClickListener, Callback, DecodeEventListener {

	private UUID[] mUuids;
	private String[] mAddresses;
	private String[] mNames;
	private CavanBleScanner mScanner;
	private CavanBleDeviceAdapter mAdapter;

	private ListView mListView;
	private SurfaceView mSurfaceView;
	private CavanQrCodeView mQrCodeView;
	private SurfaceHolder mSurfaceHolder;

	protected void onScanComplete(CavanBleDevice device) {
		CavanAndroid.dLog("onScanComplete: " + device);
		finishScan(device.getDevice());
	}

	public void finishScan(BluetoothDevice device) {
		mScanner.stopScan();

		Intent intent = new Intent();
		intent.putExtra("device", device);
		setResult(RESULT_OK, intent);
		finish();
	}

	public void startScan() {
		if (mScanner != null) {
			mScanner.startScan(mUuids, mNames, mAddresses);
			CavanAndroid.showToast(this, R.string.text_scanning);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && mScanner != null) {
			mScanner.setAutoSelect(0);
		}

		return super.onTouchEvent(event);
	}

	@Override
	protected void onCreateBle(Bundle savedInstanceState) {
		setContentView(R.layout.ble_scanner);

		mQrCodeView = (CavanQrCodeView) findViewById(R.id.cavanQrCodeView);
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceViewQrCode);
		mListView = (ListView) findViewById(R.id.listViewDevices);

		mQrCodeView.setDecodeEventListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);

		Intent intent = getIntent();
		mNames = intent.getStringArrayExtra("names");
		mAddresses = intent.getStringArrayExtra("addresses");

		Object[] objects = (Object[]) intent.getSerializableExtra("uuids");
		if (objects != null && objects.length > 0) {
			mUuids = new UUID[objects.length];

			for (int i = 0; i < objects.length; i++) {
				mUuids[i] = (UUID) objects[i];
			}
		}

		mAdapter = new CavanBleDeviceAdapter(mListView) {

			@Override
			protected void selectDevice(View view, CavanBleDevice device) {
				onScanComplete(device);
			}
		};

		mScanner = new CavanBleScanner(this) {

			@Override
			protected void onScanResult(CavanBleDevice[] devices, CavanBleDevice device) {
				CavanBleScanActivity.this.mAdapter.setDevices(devices, device);
			}

			@Override
			protected void onAutoSelected(CavanBleDevice device) {
				onScanComplete(device);
			}
		};

		if (mAddresses != null) {
			mScanner.setAutoSelect(1000);
		} else if (mUuids != null || mNames != null) {
			mScanner.setAutoSelect(5000);
		}

		if (mScanner.isAdapterEnabled()) {
			startScan();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(CavanBleScanActivity.this);
			builder.setMessage(R.string.text_bluetooth_open_prompt);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.open, CavanBleScanActivity.this);
			builder.setNegativeButton(R.string.exit, CavanBleScanActivity.this);

			Dialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	protected void onDestroy() {
		if (mScanner != null) {
			mScanner.stopScan();
		}

		super.onDestroy();
	}

	public static Intent getIntent(Context context) {
		return new Intent(context, CavanBleScanActivity.class);
	}

	public static void show(Context context) {
		context.startActivity(getIntent(context));
	}

	public static void show(Activity activity, int requestCode, UUID[] uuids, String[] names, String[] addresses) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN && activity.isDestroyed()) {
			return;
		}

		Intent intent = getIntent(activity);

		if (uuids != null && uuids.length > 0) {
			intent.putExtra("uuids", uuids);
		}

		if (names != null && names.length > 0) {
			intent.putExtra("names", names);
		}

		if (addresses != null && addresses.length > 0) {
			intent.putExtra("addresses", addresses);
		}

		activity.startActivityForResult(intent, requestCode);
	}

	public static void show(Activity activity, UUID[] uuids, String[] names, String[] addresses) {
		show(activity, 0, uuids, names, addresses);
	}

	public static void show(Activity activity, UUID[] uuids, String[] names) {
		show(activity, uuids, names, null);
	}

	public static void show(Activity activity, String[] names, String[] addresses) {
		show(activity, null, names, addresses);
	}

	public static void show(Activity activity, String[] names) {
		show(activity, null, names, null);
	}

	public static void show(Activity activity, UUID[] uuids) {
		show(activity, uuids, null);
	}

	public static void show(Activity activity) {
		show(activity, null, null, null);
	}

	public static void show(Activity activity, int requestCode) {
		show(activity, requestCode, null, null, null);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			mScanner.setAdapterEnable(true);
			startScan();
		} else {
			finish();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		CavanAndroid.pLog();

		Camera camera = mQrCodeView.openCamera(mSurfaceView.getWidth(), mSurfaceView.getHeight());
		if (camera != null) {
			try {
				camera.setPreviewDisplay(holder);
				mQrCodeView.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
				mQrCodeView.closeCamera();
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		CavanAndroid.pLog();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		CavanAndroid.pLog();
		mQrCodeView.closeCamera();
	}

	@Override
	public void onDecodeComplete(Result result) {
		if (result != null) {
			BluetoothDevice device = mScanner.getRemoteDevice(result.getText());
			if (device != null) {
				finishScan(device);
			}
		}
	}

	@Override
	public void onDecodeStart() {
		CavanAndroid.pLog();
	}
}
