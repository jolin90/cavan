package com.cavan.greendao;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import com.cavan.greendao.UserInfo;
import com.cavan.greendao.GroupInfo;

import com.cavan.greendao.UserInfoDao;
import com.cavan.greendao.GroupInfoDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig userInfoDaoConfig;
    private final DaoConfig groupInfoDaoConfig;

    private final UserInfoDao userInfoDao;
    private final GroupInfoDao groupInfoDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        userInfoDaoConfig = daoConfigMap.get(UserInfoDao.class).clone();
        userInfoDaoConfig.initIdentityScope(type);

        groupInfoDaoConfig = daoConfigMap.get(GroupInfoDao.class).clone();
        groupInfoDaoConfig.initIdentityScope(type);

        userInfoDao = new UserInfoDao(userInfoDaoConfig, this);
        groupInfoDao = new GroupInfoDao(groupInfoDaoConfig, this);

        registerDao(UserInfo.class, userInfoDao);
        registerDao(GroupInfo.class, groupInfoDao);
    }
    
    public void clear() {
        userInfoDaoConfig.getIdentityScope().clear();
        groupInfoDaoConfig.getIdentityScope().clear();
    }

    public UserInfoDao getUserInfoDao() {
        return userInfoDao;
    }

    public GroupInfoDao getGroupInfoDao() {
        return groupInfoDao;
    }

}
