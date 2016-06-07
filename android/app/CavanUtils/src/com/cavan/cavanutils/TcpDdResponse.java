package com.cavan.cavanutils;

import android.annotation.SuppressLint;

@SuppressLint("DefaultLocale")
class TcpDdResponse extends TcpDdPackage {
	private int mCode;
	private int mErrno;
	private String mMessage;

	public TcpDdResponse(int code, int number, String message) {
		super(TcpDdClient.TCP_DD_RESPONSE);
		mCode = code;
		mErrno = number;
		mMessage = message;
	}

	public TcpDdResponse() {
		super(TcpDdClient.TCP_DD_RESPONSE);
	}

	@Override
	public String toString() {
		return String.format("code = %d, errno = %d, message = %s", mCode, mErrno, mMessage);
	}

	public int getCode() {
		return mCode;
	}

	public int getErrno() {
		return mErrno;
	}

	public String getMessage() {
		return mMessage;
	}

	@Override
	protected boolean encodeBody(ByteCache cache) {
		if (!cache.writeValue32(mCode)) {
			CavanUtils.logE("Failed to writeValue32(mCode)");
			return false;
		}

		if (!cache.writeValue32(mErrno)) {
			CavanUtils.logE("Failed to writeValue32(mErrno)");
			return false;
		}

		if (mMessage != null) {
			if (!cache.writeBytes(mMessage.getBytes())) {
				CavanUtils.logE("Failed to writeBytes(mMessage)");
				return false;
			}
		}

		return true;
	}

	@Override
	public byte[] encode() {
		if (mMessage == null) {
			encode(8);
		}

		return encode(mMessage.length() + 8);
	}

	@Override
	protected boolean decodeBody(ByteCache cache) {
		if (mType != TcpDdClient.TCP_DD_RESPONSE) {
			CavanUtils.logE("Not a response type = " + mType);
			return false;
		}

		mCode = cache.readValue32();
		mErrno = cache.readValue32();
		byte[] message = cache.readBytes();
		if (message != null) {
			mMessage = new String(message);
		}

		return true;
	}
}