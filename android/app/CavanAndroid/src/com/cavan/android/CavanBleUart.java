package com.cavan.android;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.cavan.android.CavanBleChar.CavanBleDataListener;

public class CavanBleUart extends CavanBleGatt {

	public static final UUID UUID_SERVICE = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cb7");
	public static final UUID UUID_RX = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cba");
	public static final UUID UUID_TX = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cb8");
	public static final UUID UUID_OTA = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cbb");

	private CavanBleChar mCharacteristicTx;
	private CavanBleChar mCharacteristicRx;
	private CavanBleChar mCharacteristicOta;
	private CavanBleDataListener mTxDataLinstener;

	public CavanBleUart(Context context, BluetoothDevice device, UUID uuid) throws Exception {
		super(context, device, uuid);
	}

	public CavanBleUart(Context context, BluetoothDevice device) throws Exception {
		this(context, device, UUID_SERVICE);
	}

	public boolean sendData(byte[] data) throws GattInvalidStateException, TimeoutException {
		return mCharacteristicRx != null && mCharacteristicRx.writeData(data, true);
	}

	public boolean sendText(String text) throws GattInvalidStateException, TimeoutException {
		return sendData(text.getBytes());
	}

	public boolean writeOta(byte[] data) throws GattInvalidStateException, TimeoutException {
		return mCharacteristicOta != null && mCharacteristicOta.writeData(data, true);
	}

	public void setDataListener(CavanBleDataListener listener) throws GattInvalidStateException, TimeoutException {
		mTxDataLinstener = listener;
		if (mCharacteristicTx != null) {
			mCharacteristicTx.setDataListener(listener);
		}
	}

	@Override
	protected boolean doInitialize() {
		mCharacteristicRx = openChar(UUID_RX);
		if (mCharacteristicRx == null) {
			return false;
		}

		mCharacteristicTx = openChar(UUID_TX);
		if (mCharacteristicTx == null) {
			return false;
		}

		try {
			mCharacteristicTx.setDataListener(mTxDataLinstener);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		mCharacteristicOta = openChar(UUID_OTA);

		setGattReady(true);

		return true;
	}
}
