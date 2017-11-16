package com.ut.lulyfan.voicelib.voiceManager;

import android.os.Environment;

/**
 * Created by acer on 2016/12/12.
 */

public class VoiceConstant {

    public static final String path = Environment.getExternalStorageDirectory()+"/dongni.txt";

    //Dicate msg
    public static final int MSG_DICATE_LISTENBEGIN = 3300;
    public static final int MSG_DICATE_VOLUME = 3301;
    public static final int MSG_DICATE_LISTENEND = 3304;
    public static final int MSG_DICATE_RESULT = 3305;

    //Syn msg
    public static final int MSG_SYN_SPEAKBEGIN = 2201;
    public static final int MSG_SYN_SPEAKEND = 2204;

    //Destination
    public static final int MSG_DEST = 99;

    public static final int MSG_TALK = 100;

    public static final int MSG_MUTIL_DEST = 98;

    public static final int MSG_SET_SCENE = 1000;

    public static final int MSG_GET_ROBOT_IP = 1001;

}
