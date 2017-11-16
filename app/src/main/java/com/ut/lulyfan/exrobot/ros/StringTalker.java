package com.ut.lulyfan.exrobot.ros;

/**
 * Created by Administrator on 2017/10/26/026.
 */

public class StringTalker extends Talker<std_msgs.String>{

    int count;

    private String data;

    public StringTalker(String topic) {
        super(topic, std_msgs.String._TYPE);
    }

    public void sendMsg(String data) {
        sendMsg(data, 1);
    }

    public void sendMsg(String data, int sendCount) {
        sendMsg(data, sendCount, 1000);
    }

    public void sendMsg(String data, int sendCount, long gapTime) {

        this.data = data;
        this.gapTime = gapTime;
        this.sendCount = sendCount;
    }

    @Override
    protected void editMsg(std_msgs.String msg) {
        msg.setData(data + count++);
    }



}
