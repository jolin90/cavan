package com.cavan.cavanmain;

import android.net.Uri;
import android.os.Environment;

import com.cavan.android.CavanDatabaseProvider;

public class CavanNotificationProvider extends CavanDatabaseProvider {

	public static final String AUTHORIT = "com.cavan.notification.provider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORIT);

	private static final String DB_NAME = Environment.getExternalStorageDirectory().getPath() + "/CavanMain.db";
	private static final int DB_VERSION = 3;

	@Override
	protected void initTables() {
		CavanNotification.initDatabaseTable(this);
		CavanSetting.initDatabaseTable(this);
		CavanFilter.initDatabaseTable(this);
	}

	@Override
	protected String getAuthority() {
		return AUTHORIT;
	}

	@Override
	protected String getDatabaseName() {
		return DB_NAME;
	}

	@Override
	protected int getDatabaseVersion() {
		return DB_VERSION;
	}
}
