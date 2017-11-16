package com.ut.lulyfan.exrobot.ros;

import std_msgs.Bool;

/**
 * Created by Administrator on 2017/10/31/031.
 */

public class BoolTalker extends Talker<Bool> {

    private boolean data;

    public BoolTalker(String topic) {
        super(topic, std_msgs.Bool._TYPE);
    }

    public void sendMsg(boolean data) {
        sendMsg(data, 1);
    }

    public void sendMsg(boolean data, int sendCount) {
        sendMsg(data, sendCount, 1000);
    }

    public void sendMsg(boolean data, int sendCount, long gapTime) {

        this.data = data;
        this.gapTime = gapTime;
        this.sendCount = sendCount;
    }

    @Override
    protected void editMsg(Bool msg) {
        msg.setData(data);
    }
}
