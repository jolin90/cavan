package com.cavan.resource;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;

import com.cavan.android.CavanCheckBox;

public class EditableMultiSelectListPreference extends DialogPreference implements OnClickListener, OnCheckedChangeListener {

	public class Entry implements OnCheckedChangeListener {

		private String mKeyword;
		private boolean mEnabled;

		public Entry(String keyword, boolean enable) {
			mKeyword = keyword;
			mEnabled = enable;
		}

		public Entry(String text) {
			if (text.length() > 0 && text.charAt(0) == '!') {
				mKeyword = text.substring(1);
				mEnabled = false;
			} else {
				mKeyword = text;
				mEnabled = true;
			}
		}

		public String getKeyword() {
			return mKeyword;
		}

		public void setKeyword(String keyword) {
			mKeyword = keyword;
		}

		public boolean isEnabled() {
			return mEnabled;
		}

		public void setEnable(boolean enable) {
			mEnabled = enable;
		}

		public StringBuilder append(StringBuilder builder) {
			if (!mEnabled) {
				builder.append('!');
			}

			builder.append(mKeyword);

			return builder;
		}

		@Override
		public String toString() {
			if (mEnabled) {
				return mKeyword;
			}

			return '!' + mKeyword;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mEnabled = isChecked;

			if (isChecked) {
				isChecked = isAllEnabled();
			}

			mCheckBoxSelectAll.setCheckedSilent(isChecked);
		}
	}

	private Button mButtonAdd;
	private Button mButtonRemove;
	private EditText mEditTextKeyword;
	private ListView mListViewKeywords;
	private CavanCheckBox mCheckBoxSelectAll;

	private ArrayList<Entry> mEntries = new ArrayList<Entry>();
	private BaseAdapter mAdapter = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CavanCheckBox view;

			if (convertView != null) {
				view = (CavanCheckBox) convertView;
			} else {
				view = new CavanCheckBox(getContext());
			}

			Entry entry = mEntries.get(position);
			view.setText(entry.getKeyword());
			view.setCheckedSilent(entry.isEnabled());
			view.setOnCheckedChangeListener(entry);
			return view;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return mEntries.get(position);
		}

		@Override
		public int getCount() {
			return mEntries.size();
		}
	};

	public EditableMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public EditableMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public EditableMultiSelectListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditableMultiSelectListPreference(Context context) {
		super(context);
	}

	private static String[] loadPrivate(SharedPreferences preferences, String key) {
		try {
			String lines = preferences.getString(key, null);
			if (lines != null) {
				return lines.split("\\s*\\n\\s*");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static ArrayList<String> load(SharedPreferences preferences, String key) {
		String[] lines = loadPrivate(preferences, key);
		if (lines == null) {
			return null;
		}

		ArrayList<String> list = new ArrayList<String>();

		for (String line : lines) {
			if (line.length() > 0 && line.charAt(0) != '!') {
				list.add(line);
			}
		}

		return list;
	}

	public static ArrayList<String> load(Context context, String key) {
		return load(PreferenceManager.getDefaultSharedPreferences(context), key);
	}

	private boolean load() {
		String key = getKey();
		if (key == null || key.isEmpty()) {
			return false;
		}

		String[] lines = loadPrivate(getSharedPreferences(), key);
		if (lines == null) {
			return false;
		}

		for (String line : lines) {
			Entry entry = new Entry(line);
			mEntries.add(entry);
		}

		return true;
	}

	private boolean save() {
		StringBuilder builder = new StringBuilder();

		for (Entry entry : mEntries) {
			if (builder.length() > 0) {
				builder.append('\n');
			}

			entry.append(builder);
		}

		return persistString(builder.toString());
	}

	private boolean add() {
		Editable text = mEditTextKeyword.getText();
		if (text != null && text.length() > 0) {
			mEntries.add(new Entry(text.toString(), true));
			mAdapter.notifyDataSetChanged();
			text.clear();
			return true;
		}

		return false;
	}

	private int remove() {
		int count = 0;

		Iterator<Entry> iterator = mEntries.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (entry.isEnabled()) {
				iterator.remove();
				count++;
			}
		}

		if (count > 0) {
			mAdapter.notifyDataSetChanged();
		}

		return count;
	}

	private boolean isAllEnabled() {
		for (Entry entry : mEntries) {
			if (!entry.isEnabled()) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		View view = LayoutInflater.from(getContext()).inflate(R.layout.editable_multi_select_list_preference, null);

		mButtonAdd = (Button) view.findViewById(R.id.buttonAdd);
		mButtonAdd.setOnClickListener(this);

		mButtonRemove = (Button) view.findViewById(R.id.buttonRemove);
		mButtonRemove.setOnClickListener(this);

		mEditTextKeyword = (EditText) view.findViewById(R.id.editTextKeyword);
		mListViewKeywords = (ListView) view.findViewById(R.id.listViewKeywords);
		mListViewKeywords.setAdapter(mAdapter);

		mCheckBoxSelectAll = (CavanCheckBox) view.findViewById(R.id.checkBoxSelectAll);
		mCheckBoxSelectAll.setCheckedSilent(isAllEnabled());
		mCheckBoxSelectAll.setOnCheckedChangeListener(this);

		builder.setView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			add();

			if (callChangeListener(mEntries)) {
				save();
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			load();
		} else {
			for (String text : ((String) defaultValue).split("\\s*,\\s*")) {
				mEntries.add(new Entry(text));
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mButtonAdd) {
			add();
		} else if (v == mButtonRemove) {
			remove();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		for (Entry entry : mEntries) {
			entry.setEnable(isChecked);
		}

		mAdapter.notifyDataSetChanged();
	}
}
