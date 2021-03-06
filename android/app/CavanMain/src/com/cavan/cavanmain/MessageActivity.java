package com.cavan.cavanmain;

import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;

import com.cavan.android.CavanAndroid;
import com.cavan.cavanmain.R;

public class MessageActivity extends Activity {

	private CavanMessageAdapter mAdapter;
	private CavanMessageFilter mMessageFinder = new CavanMessageFilter();
	private ContentObserver mContentObserverMessage = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			updateData(uri, false);
		}
	};

	private boolean mFilterEnable;
	private String mSelection;
	private String[] mSelectionArgs;
	private Pattern[] mFilterPatterns;

	public static Intent getIntent(Context context) {
		return new Intent(context, MessageActivity.class);
	}

	public Pattern[] getFilterPatterns() {
		return mFilterPatterns;
	}

	public void updateData(Uri uri, boolean bottom) {
		mAdapter.updateData(uri, mSelection, mSelectionArgs, bottom);
	}

	private void updateFilter() {
		CavanFilter[] filters;

		if (mFilterEnable) {
			filters = CavanFilter.queryFilterEnabled(getContentResolver());
		} else {
			filters = null;
		}

		if (filters != null && filters.length > 0) {
			mFilterPatterns = new Pattern[filters.length];
			StringBuilder builder = new StringBuilder();

			mSelectionArgs = new String[filters.length];

			for (int i = 0; i < filters.length; i++) {
				String text = filters[i].getContent();

				mFilterPatterns[i] = Pattern.compile(text, Pattern.CASE_INSENSITIVE);

				if (i > 0) {
					builder.append(" or ");
				}

				builder.append(CavanNotification.KEY_CONTENT + " like ?");
				mSelectionArgs[i] = "%" + text + "%";
			}

			builder.append(" collate nocase");

			mSelection = builder.toString();
		} else {
			mSelection = null;
			mSelectionArgs = null;
			mFilterPatterns = null;
		}

		updateData(null, true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getIntent().getData();
		if (uri != null) {
			CavanAndroid.dLog("uri = " + uri);

			try {
				List<String> paths = uri.getPathSegments();
				String action = paths.get(0);

				if (CavanMessageView.ACTION_ALIPAY.equals(action)) {
					String code = paths.get(1);

					RedPacketListenerService.postRedPacketCode(this, paths.get(1));
					RedPacketListenerService.startAlipayActivity(this);

					CavanAndroid.showToastLong(this, "支付宝口令: " + code);
				} else if (CavanMessageView.ACTION_OPEN.equals(action)) {
					CavanAndroid.startActivity(this, uri.getHost());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			finish();
		} else {
			setContentView(R.layout.notification_activity);
			CavanAndroid.setMenuKeyVisibility(getWindow(), true);

			mAdapter = new CavanMessageAdapter(this);
			updateData(null, true);

			getContentResolver().registerContentObserver(CavanNotification.CONTENT_URI, true, mContentObserverMessage);
		}
	}

	@Override
	protected void onDestroy() {
		if (mContentObserverMessage != null) {
			getContentResolver().unregisterContentObserver(mContentObserverMessage);
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.message_activity, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_message_clean:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.clean_message_confirm);
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					int count = CavanNotification.deleteAll(getContentResolver());
					CavanAndroid.showToast(getApplicationContext(), String.format("成功清除 %d 条消息", count));
					updateData(null, true);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.create().show();

			break;

		case R.id.action_message_finder:
			mMessageFinder.show(getFragmentManager());
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public class CavanMessageFilter extends DialogFragment implements OnCheckedChangeListener, OnClickListener, DialogInterface.OnClickListener {

		private CavanFilter[] mFilters;

		private Button mButtonAdd;
		private Button mButtonDelete;
		private ListView mListViewFilter;
		private EditText mEditTextFilter;
		private CheckBox mCheckBoxSelectAll;

		private BaseAdapter mFilterAdapter = new BaseAdapter() {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CavanFilterView view = (CavanFilterView) convertView;
				CavanFilter filter = mFilters[position];
				if (view == null) {
					view = new CavanFilterView(MessageActivity.this, filter);
				} else {
					view.setFilter(filter);
				}

				return view;
			}

			@Override
			public long getItemId(int position) {
				return 0;
			}

			@Override
			public Object getItem(int position) {
				return null;
			}

			@Override
			public int getCount() {
				if (mFilters != null) {
					return mFilters.length;
				}

				return 0;
			}

			@Override
			synchronized public void notifyDataSetChanged() {
				mFilters = CavanFilter.queryFilter(getContentResolver());
				super.notifyDataSetChanged();
			}
		};

		public void show(FragmentManager manager) {
			super.show(manager, CavanAndroid.TAG);
		}

		public void addFilter(String text) {
			if (!text.isEmpty()) {
				CavanFilter filter = new CavanFilter(text, true);
				filter.update(getContentResolver());
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			View view = getLayoutInflater().inflate(R.layout.message_filter, null);

			mCheckBoxSelectAll = (CheckBox) view.findViewById(R.id.checkBoxSelectAll);
			// mCheckBoxSelectAll.setTextColor(Color.WHITE);
			mCheckBoxSelectAll.setOnCheckedChangeListener(this);

			mButtonAdd = (Button) view.findViewById(R.id.buttonAdd);
			mButtonAdd.setOnClickListener(this);

			mButtonDelete = (Button) view.findViewById(R.id.buttonDelete);
			mButtonDelete.setOnClickListener(this);

			mEditTextFilter = (EditText) view.findViewById(R.id.editTextFilter);

			mListViewFilter = (ListView) view.findViewById(R.id.listViewFilter);
			mListViewFilter.setAdapter(mFilterAdapter);
			mFilterAdapter.notifyDataSetChanged();

			AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);

			builder.setView(view);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.filter, this);
			builder.setNegativeButton(R.string.filter_none, this);

			return builder.create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				mFilterEnable = true;
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				mFilterEnable = false;
				break;
			}
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			addFilter(mEditTextFilter.getText().toString());
			updateFilter();

			super.onDismiss(dialog);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.buttonAdd:
				Editable editable = mEditTextFilter.getText();
				addFilter(editable.toString());
				mFilterAdapter.notifyDataSetChanged();
				editable.clear();
				break;

			case R.id.buttonDelete:
				CavanFilter.deleteEnabled(getContentResolver());
				mFilterAdapter.notifyDataSetChanged();
				break;
			}
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (mFilters != null) {
				for (int i = mFilters.length - 1; i >= 0; i--) {
					mFilters[i].setEnable(getContentResolver(), isChecked);
				}

				mFilterAdapter.notifyDataSetChanged();
			}
		}
	}

	public class CavanFilterView extends CheckBox implements OnCheckedChangeListener {

		private CavanFilter mFilter;

		public CavanFilterView(Context context, CavanFilter filter) {
			super(context);

			// setTextColor(Color.WHITE);
			setFilter(filter);

			setOnCheckedChangeListener(this);
		}

		public void setFilter(CavanFilter filter) {
			mFilter = filter;

			setText(mFilter.getContent());
			setChecked(mFilter.isEnabled());
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mFilter.setEnable(getContentResolver(), isChecked);
		}
	}
}
