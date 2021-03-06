package com.cavan.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cavan.java.CavanMacAddress;
import com.cavan.java.CavanString;

public class CavanMacAddressView extends LinearLayout {

	private static int TEXT_MAX_LENGTH = 2;

	private TextWatcher mWatcher;
	private int mKeyboard;
	private CavanKeyboardView mKeyboardView;
	private CavanMacAddress mAddress = new CavanMacAddress();
	private CavanMacAddressSubView[] mSubViews = new CavanMacAddressSubView[6];

	public class CavanMacAddressSubView extends EditText implements InputFilter, OnFocusChangeListener {

		private int mIndex;

		private Runnable mRunnableGotoNextView = new Runnable() {

			@Override
			public void run() {
				gotoNextView();
			}
		};

		public CavanMacAddressSubView(Context context, int index) {
			super(context);

			mIndex = index;

			setGravity(Gravity.CENTER);
			setMinEms(1);
			setText("00");

			if (mKeyboardView != null) {
				mKeyboardView.setupEditText(this);
				setOnFocusChangeListener(this);
			}

			setFilters(new InputFilter[] { this });

			if (mWatcher != null) {
				addTextChangedListener(mWatcher);
			}
		}

		private void setInputType() {
			if (mKeyboardView != null) {
				mKeyboardView.setupEditText(this);
				setOnFocusChangeListener(this);
				mKeyboardView.setKeyboard(this, mKeyboard);
			} else {
				setInputType(InputType.TYPE_CLASS_NUMBER);
			}
		}

		public String getString() {
			return getText().toString();
		}

		public int length() {
			return getEditableText().length();
		}

		public boolean isEmpty() {
			return length() <= 0;
		}

		public void clear() {
			getEditableText().clear();
		}

		public CavanMacAddressSubView getNextView() {
			int index = mIndex + 1;
			if (index < mSubViews.length) {
				return mSubViews[index];
			}

			return null;
		}

		public boolean gotoNextView() {
			CavanMacAddressSubView view = getNextView();
			if (view != null) {
				view.requestFocus();
				return true;
			}

			selectAll();

			return false;
		}

		public CavanMacAddressSubView getPrevView() {
			if (mIndex > 0) {
				return mSubViews[mIndex - 1];
			}

			return null;
		}

		public boolean gotoPrevView() {
			CavanMacAddressSubView view = getPrevView();
			if (view != null) {
				view.requestFocus();
				return true;
			}

			selectAll();

			return true;
		}

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			for (int i = start; i < end; i++) {
				if (!CavanString.isNumber(source.charAt(i), 16)) {
					return CavanString.EMPTY_STRING;
				}
			}

			int sLen = end - start;
			int dLen = dend - dstart;

			if (sLen > dLen) {
				int length = dest.length() - dLen;
				if (length + sLen >= TEXT_MAX_LENGTH) {
					end = start + (TEXT_MAX_LENGTH - length);

					if (isFocused()) {
						postDelayed(mRunnableGotoNextView, 100);
					}
				}
			}

			return source.subSequence(start, end).toString().toUpperCase();
		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				if (mKeyboardView != null) {
					mKeyboardView.setEditText(this);
				}
			} else {
				Editable editable = getEditableText();
				while (editable.length() < TEXT_MAX_LENGTH) {
					editable.insert(0, "0");
				}
			}
		}
	};

	public CavanMacAddressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public CavanMacAddressView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public CavanMacAddressView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CavanMacAddressView(Context context) {
		super(context);
	}

	public void setKeyboardView(CavanKeyboardView view, int keyboard) {
		mKeyboardView = view;
		mKeyboard = keyboard;

		for (CavanMacAddressSubView sub : mSubViews) {
			if (sub != null) {
				sub.setInputType();
			}
		}
	}

	public CavanMacAddressSubView getSubView(int index) {
		return mSubViews[index];
	}

	public CavanMacAddressSubView[] getSubViews(int index) {
		return mSubViews;
	}

	public boolean requestFocusForSubView(int index) {
		if (index >= 0 && index < mSubViews.length) {
			CavanMacAddressSubView view = mSubViews[index];
			if (view == null) {
				return false;
			}

			return view.requestFocus();
		}

		return false;
	}

	public String[] getTexts() {
		String[] texts = new String[mSubViews.length];

		for (int i = texts.length - 1; i >= 0; i--) {
			CavanMacAddressSubView view = mSubViews[i];
			if (view != null) {
				texts[i] = view.getString();
			} else {
				texts[i] = "0";
			}
		}

		return texts;
	}

	public void setTexts(String[] texts) {
		for (int i = mSubViews.length - 1, j = texts.length - 1; i >= 0 && j >= 0; i--, j--) {
			CavanMacAddressSubView view = mSubViews[i];
			if (view != null) {
				view.setText(texts[j]);
			}
		}
	}

	public CavanMacAddress getAddress() {
		return mAddress.fromStrings(getTexts());
	}

	public void setAddress(CavanMacAddress address) {
		setTexts(address.toStrings());
	}

	public void setAddress(String address) {
		mAddress.fromString(address);
		setAddress(mAddress);
	}

	public void setAddress(BluetoothDevice device) {
		setAddress(device.getAddress());
	}

	public BluetoothDevice getBluetoothDevice(BluetoothAdapter adapter) {
		return adapter.getRemoteDevice(getAddress().toString());
	}

	public void clear() {
		for (CavanMacAddressSubView view : mSubViews) {
			if (view != null) {
				view.clear();
			}
		}

		if (mSubViews[0] != null) {
			mSubViews[0].requestFocus();
		}
	}

	public void addTextChangedListener(TextWatcher watcher) {
		mWatcher = watcher;

		for (CavanMacAddressSubView view : mSubViews) {
			if (view != null) {
				view.addTextChangedListener(watcher);
			}
		}
	}

	@Override
	public String toString() {
		CavanMacAddress address = getAddress();
		return address.toString();
	}

	@Override
	protected void onFinishInflate() {
		for (int i = 0; i < mSubViews.length; i++) {
			if (i > 0) {
				TextView textView = new TextView(getContext());
				textView.getPaint().setFakeBoldText(true);
				textView.setGravity(Gravity.CENTER);
				textView.setText(":");
				addView(textView);
			}

			CavanMacAddressSubView editText = new CavanMacAddressSubView(getContext(), i);
			mSubViews[i] = editText;
			addView(editText);
		}

		super.onFinishInflate();
	}
}
