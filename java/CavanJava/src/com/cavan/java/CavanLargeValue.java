package com.cavan.java;

public class CavanLargeValue implements Cloneable, Comparable<CavanLargeValue> {

	public static int findMsbIndex(byte[] bytes, int msb) {
		while (msb >= 0 && bytes[msb] == 0) {
			msb--;
		}

		return msb;
	}

	public static int findMsbIndex(byte[] bytes) {
		return findMsbIndex(bytes, bytes.length - 1);
	}

	public static int findLsbIndex(byte[] bytes, int lsb) {
		while (lsb < bytes.length && bytes[lsb] == 0) {
			lsb++;
		}

		return lsb;
	}

	public static int findLsbIndex(byte[] bytes) {
		return findLsbIndex(bytes, 0);
	}

	public static boolean isZero(byte[] bytes) {
		for (byte value : bytes) {
			if (value != 0) {
				return false;
			}
		}

		return true;
	}

	public static boolean notZero(byte[] bytes) {
		for (byte value : bytes) {
			if (value != 0) {
				return true;
			}
		}

		return false;
	}

	public static byte getLastByte(byte[] bytes) {
		return bytes[bytes.length - 1];
	}

	public static boolean isNegative(byte[] bytes) {
		return (getLastByte(bytes) & (1 << 7)) != 0;
	}

	public static boolean isPositive(byte[] bytes) {
		return (getLastByte(bytes) & (1 << 7)) == 0;
	}

	public static void clear(byte[] bytes, int index, int end) {
		while (index < end) {
			bytes[index++] = 0;
		}
	}

	public static void clear(byte[] bytes, int index) {
		clear(bytes, index, bytes.length);
	}

	public static void clear(byte[] bytes) {
		clear(bytes, 0);
	}

	public static int compare(byte[] bytes1, int msb1, byte[] bytes2, int msb2) {
		if (msb1 > msb2) {
			return 1;
		}

		if (msb1 < msb2) {
			return -1;
		}

		for (int i = msb1; i >= 0; i--) {
			if (bytes1[i] != bytes2[i]) {
				return (bytes1[i] & 0xFF) - (bytes2[i] & 0xFF);
			}
		}

		return 0;
	}

	public static int compare(byte[] bytes1, byte[] bytes2) {
		return compare(bytes1, findMsbIndex(bytes1), bytes2, findMsbIndex(bytes2));
	}

	public static int increase(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] != (byte) 0xFF) {
				bytes[i]++;
				return 0;
			}

			bytes[i] = 0;
		}

		return 1;
	}

	public static int decrease(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] != 0) {
				bytes[i]--;
				return 0;
			}

			bytes[i] = (byte) 0xFF;
		}

		return -1;
	}

	public static long add(byte[] bytes, long value) {
		for (int i = 0; i < bytes.length && value != 0; i++) {
			value += bytes[i] & 0xFF;
			bytes[i] = (byte) value;
			value >>= 8;
		}

		return value;
	}

	public static long sub(byte[] bytes, long value) {
		return add(bytes, -value);
	}

	public static long mul(byte[] bytes, long value) {
		long carry = 0;

		for (int i = findLsbIndex(bytes); i < bytes.length; i++) {
			carry += (bytes[i] & 0xFF) * value;
			bytes[i] = (byte) carry;
			carry >>= 8;
		}

		return carry;
	}

	public static long div(byte[] bytes, long value) {
		long remain = 0;

		for (int i = findMsbIndex(bytes); i >= 0; i--) {
			remain = (remain << 8) | (bytes[i] & 0xFF);
			bytes[i] = (byte) (remain / value);
			remain %= value;
		}

		return remain;
	}

	public static int add(byte[] bytes1, byte[] bytes2) {
		int carry = 0;

		for (int i = 0, j = 0; i < bytes1.length; i++) {
			if (j < bytes2.length) {
				carry += bytes2[j++] & 0xFF;
			} else if (carry == 0) {
				return 0;
			}

			carry += bytes1[i] & 0xFF;
			bytes1[i] = (byte) carry;
			carry >>= 8;
		}

		return carry;
	}

	public static int sub(byte[] bytes1, byte[] bytes2) {
		int carry = 0;

		for (int i = 0, j = 0; i < bytes1.length; i++) {
			if (j < bytes2.length) {
				carry -= bytes2[j++] & 0xFF;
			} else if (carry == 0) {
				return 0;
			}

			carry += bytes1[i] & 0xFF;
			bytes1[i] = (byte) carry;
			carry >>= 8;
		}

		return carry;
	}

	public static byte[] mul(byte[] bytes1, int msb1, byte[] bytes2, int msb2) {
		byte[] bytes = new byte[msb1 + msb2 + 2];

		for (int i = 0; i <= msb1; i++) {
			int value = bytes1[i] & 0xFF;
			int carry = 0;
			int k = i;

			for (int j = 0; j <= msb2; j++, k++) {
				carry += (bytes[k] & 0xFF) + (bytes2[j] & 0xFF) * value;
				bytes[k] = (byte) carry;
				carry >>= 8;
			}

			bytes[k] = (byte) carry;
		}

		return bytes;
	}

	public static byte[] mul(byte[] bytes1, byte[] bytes2) {
		return mul(bytes1, findMsbIndex(bytes1), bytes2, findMsbIndex(bytes2));
	}

	// ============================================================

	private byte[] mBytes;

	public CavanLargeValue(byte[] bytes) {
		mBytes = bytes;
	}

	public CavanLargeValue(int length) {
		this(new byte[length]);
	}

	public CavanLargeValue(CavanLargeValue value) {
		this(value.getBytes().clone());
	}

	// ============================================================

	public int findMsbIndex(int msb) {
		return findMsbIndex(mBytes, msb);
	}

	public int findMsbIndex() {
		return findMsbIndex(mBytes);
	}

	public int findLsbIndex(int lsb) {
		return findLsbIndex(mBytes, lsb);
	}

	public int findLsbIndex() {
		return findLsbIndex(mBytes);
	}

	public void clear() {
		clear(mBytes);
	}

	public void setBytes(byte[] bytes, int msb) {
		mBytes = bytes;
	}

	public void setBytes(byte[] bytes) {
		setBytes(bytes, bytes.length - 1);
	}

	public byte[] getBytes() {
		return mBytes;
	}

	public byte[] getBytes(int length) {
		if (mBytes == null || mBytes.length < length) {
			mBytes = new byte[length];
		} else {
			clear(mBytes, length);
		}

		return mBytes;
	}

	public byte getByte(int index) {
		return mBytes[index];
	}

	public void setByte(int index, byte value) {
		mBytes[index] = value;
	}

	public byte getFirstByte() {
		return mBytes[0];
	}

	public byte getLastByte() {
		return mBytes[mBytes.length - 1];
	}

	public int length() {
		return mBytes.length;
	}

	public void setLength(int length) {
		if (length == mBytes.length) {
			return;
		}

		byte[] bytes = new byte[length];

		if (length > mBytes.length) {
			length = mBytes.length;
			clear(bytes, length);
		}

		System.arraycopy(mBytes, 0, bytes, 0, length);
		mBytes = bytes;
	}

	public boolean isZero() {
		return isZero(mBytes);
	}

	public boolean notZero() {
		return notZero(mBytes);
	}

	public boolean isNegative() {
		return isNegative(mBytes);
	}

	public boolean isPositive() {
		return isPositive(mBytes);
	}

	// ============================================================

	public int increase() {
		return increase(mBytes);
	}

	public int decrease() {
		return decrease(mBytes);
	}

	public long add(long value) {
		return add(mBytes, value);
	}

	public static CavanLargeValue add(CavanLargeValue a, long value) {
		a = a.clone();
		a.add(value);
		return a;
	}

	public int add(byte[] bytes) {
		return add(mBytes, bytes);
	}

	public static CavanLargeValue add(CavanLargeValue a, byte[] bytes) {
		a = a.clone();
		a.add(bytes);
		return a;
	}

	public int add(CavanLargeValue value) {
		return add(value.getBytes());
	}

	public static CavanLargeValue add(CavanLargeValue a, CavanLargeValue b) {
		a = a.clone();
		a.add(b);
		return a;
	}

	public long sub(long value) {
		return add(-value);
	}

	public static CavanLargeValue sub(CavanLargeValue a, long value) {
		a = a.clone();
		a.sub(value);
		return a;
	}

	public int sub(byte[] bytes) {
		return sub(mBytes, bytes);
	}

	public static CavanLargeValue sub(CavanLargeValue a, byte[] bytes) {
		a = a.clone();
		a.sub(bytes);
		return a;
	}

	public int sub(CavanLargeValue value) {
		return sub(value.getBytes());
	}

	public static CavanLargeValue sub(CavanLargeValue a, CavanLargeValue b) {
		a = a.clone();
		a.sub(b);
		return a;
	}

	public long mul(long value) {
		return mul(mBytes, value);
	}

	public static CavanLargeValue mul(CavanLargeValue a, long value) {
		a = a.clone();
		a.mul(value);
		return a;
	}

	public byte[] mul(byte[] bytes) {
		return mul(mBytes, bytes);
	}

	public static CavanLargeValue mul(CavanLargeValue a, byte[] bytes) {
		return new CavanLargeValue(a.mul(bytes));
	}

	public CavanLargeValue mul(CavanLargeValue value) {
		return new CavanLargeValue(mul(value.getBytes()));
	}

	public static CavanLargeValue mul(CavanLargeValue a, CavanLargeValue b) {
		return a.mul(b);
	}

	public long div(long value) {
		return div(mBytes, value);
	}

	public static CavanLargeValue div(CavanLargeValue a, long value) {
		a = a.clone();
		a.div(value);
		return a;
	}

	// ============================================================

	public CavanLargeValue fromLong(long value) {
		for (int i = 0; i < mBytes.length; i++, value >>= 8) {
			mBytes[i] = (byte) value;
		}

		return this;
	}

	public CavanLargeValue fromDouble(double value) {
		return fromLong((long) value);
	}

	public CavanLargeValue fromBytes(byte[] bytes, int index, int end) {
		int length = end - index;

		System.arraycopy(bytes, index, getBytes(length), 0, length);
		return this;
	}

	public CavanLargeValue fromBytes(byte[] bytes, int index) {
		return fromBytes(bytes, index, bytes.length);
	}

	public CavanLargeValue fromBytes(byte... args) {
		mBytes = args.clone();
		return this;
	}

	public CavanLargeValue fromStrings(String[] texts, int index, int end, int radix) {
		int length = end - index;
		byte[] bytes = getBytes(length);

		for (int i = length - 1; i >= 0; i--, index++) {
			try {
				bytes[i] = (byte) Integer.parseInt(texts[index], radix);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		return this;
	}

	public CavanLargeValue fromStrings(String[] texts, int index, int end) {
		return fromStrings(texts, index, end, 16);
	}

	public CavanLargeValue fromStrings(String[] texts, int index) {
		return fromStrings(texts, index, texts.length);
	}

	public CavanLargeValue fromStrings(String... args) {
		return fromStrings(args, 0);
	}

	public CavanLargeValue fromStrings(int radix, String... args) {
		return fromStrings(args, 0, args.length, radix);
	}

	public CavanLargeValue fromValues(int[] values, int index, int end) {
		int length = end - index;
		byte[] bytes = getBytes(length);

		for (int i = length - 1; i >= 0 && index < end; i--, index++) {
			bytes[i] = (byte) values[index];
		}

		return this;
	}

	public CavanLargeValue fromValues(int[] values, int index) {
		return fromValues(values, index, values.length);
	}

	public CavanLargeValue fromValues(int... args) {
		return fromValues(args, 0);
	}

	public CavanLargeValue fromString(String text, int index, int end, int radix) {
		clear();

		while (index < end) {
			int value = CavanString.convertCharToValue(text.charAt(index));
			if (value < 0 || value >= radix) {
				break;
			}

			mul(radix);
			add(value);
			index++;
		}

		return this;
	}

	public CavanLargeValue fromString(String text, int index, int end) {
		return fromString(text, index, end, 10);
	}

	public CavanLargeValue fromString(String text, int index) {
		return fromString(text, index, text.length());
	}

	public CavanLargeValue fromString(String text) {
		return fromString(text, 0);
	}

	// ============================================================

	public long toLong() {
		long value = 0;

		for (int i = mBytes.length - 1; i >= 0; i--) {
			value = value << 8 | (mBytes[i] & 0xFF);
		}

		return value;
	}

	public double toDouble() {
		return toLong();
	}

	public String toStringHex() {
		char[] chars = new char[mBytes.length * 2];

		for (int i = mBytes.length - 1, j = 0; i >= 0; i--, j += 2) {
			CavanString.fromByte(chars, j, mBytes[i]);
		}

		return new String(chars);
	}

	public String toStringBin() {
		char[] chars = new char[mBytes.length * 8];

		for (int i = mBytes.length - 1, j = 0; i >= 0; i--) {
			byte value = mBytes[i];

			for (int k = 7; k >= 0; j++, k--) {
				chars[j] = CavanString.convertValueToCharUppercase((value >> k) & 1);
			}
		}

		return new String(chars);
	}

	public String toString(int radix) {
		char[] chars = new char[length() * 8];
		CavanLargeValue value = clone();
		int length;

		for (length = 0; value.notZero(); length++) {
			int remain = (int) value.div(radix);
			chars[length] = CavanString.convertValueToCharUppercase(remain);
		}

		if (length > 0) {
			CavanArray.reverse(chars, 0, length);
			return new String(chars, 0, length);
		}

		return "0";
	}

	@Override
	public String toString() {
		return toString(10);
	}

	// ============================================================

	public CavanLargeValue clone() {
		return new CavanLargeValue(this);
	}

	@Override
	public int compareTo(CavanLargeValue o) {
		return compare(mBytes, o.getBytes());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CavanLargeValue) {
			return compareTo((CavanLargeValue) obj) == 0;
		} else if (obj instanceof byte[]) {
			return compare(getBytes(), (byte[]) obj) == 0;
		}

		return false;
	}

	public static void main(String[] args) {
		CavanLargeValue a = new CavanLargeValue(8).fromString("12345678");
		CavanLargeValue b = new CavanLargeValue(8).fromString("0");

		CavanJava.dLog("a = " + a);
		CavanJava.dLog("b = " + b);

		CavanJava.dLog("add = " + CavanLargeValue.add(a, 10000));
		CavanJava.dLog("add = " + CavanLargeValue.add(a, b));

		CavanJava.dLog("sub = " + CavanLargeValue.sub(a, 10000));
		CavanJava.dLog("sub = " + CavanLargeValue.sub(a, b));

		CavanJava.dLog("mul = " + CavanLargeValue.mul(a, 10000));
		CavanJava.dLog("mul = " + CavanLargeValue.mul(a, b));

		CavanJava.dLog("div = " + CavanLargeValue.div(a, 10000));
	}
}
