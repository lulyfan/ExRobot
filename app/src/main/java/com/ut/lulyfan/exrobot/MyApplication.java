package com.ut.lulyfan.exrobot;

import android.os.Environment;
import android.support.multidex.MultiDexApplication;

import com.ut.lulyfan.exrobot.debug.CrashHandler;
import com.ut.lulyfan.exrobot.util.CustomerDBUtil;
import com.ut.lulyfan.voicelib.voiceManager.DongniVoiceHelper;

import org.greenrobot.greendao.database.Database;

import java.io.File;

import greenDao.DaoMaster;
import greenDao.DaoSession;

/**
 * Created by Administrator on 2017/10/25/025.
 */

public class MyApplication extends MultiDexApplication{
    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/UTRobot";
        File file = new File(dir);
        if (!file.exists())
            file.mkdirs();
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, dir + "/ExRobot_db");
        Database database = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(database).newSession();
        CustomerDBUtil.getInstance(daoSession);


        DongniVoiceHelper.loadVoice(this);
    }
}
