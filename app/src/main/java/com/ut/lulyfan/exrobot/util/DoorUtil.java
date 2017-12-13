package com.ut.lulyfan.exrobot.util;

import android.content.Context;

import com.android.udi.DoorManager;

/**
 * Created by Administrator on 2017/11/28/028.
 */

public class DoorUtil {

    public final static int OPENED = 1;
    public final static int CLOSED = 0;
    DoorManager doorManager;

    public DoorUtil(Context context) {
        doorManager = (DoorManager) context.getSystemService("udi-door");
    }

    public void open() {
        doorManager.setValue(1);

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        doorManager.setValue(0);
    }

    public int check() {
        return doorManager.getValue();
    }
}
