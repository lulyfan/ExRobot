package com.ut.lulyfan.exrobot.ros;

import geometry_msgs.PoseStamped;

/**
 * Created by Administrator on 2017/10/26/026.
 */

public class GoTalker extends Talker<PoseStamped> {

    private double x;
    private double y;
    private double w;
    private double z;

    public GoTalker(String topic) {
        super(topic, PoseStamped._TYPE);
    }

    public void sendMsg(double x, double y, double z, double w ) {
        sendMsg(x, y, w, z, 1);
    }

    public void sendMsg(double x, double y, double z, double w, int sendCount) {
        sendMsg(x, y, w, z, sendCount, 1000);
    }

    public void sendMsg(double x, double y, double z, double w, int sendCount, long gapTime) {

        this.x = x;
        this.y = y;
        this.w = w;
        this.z = z;
        this.gapTime = gapTime;
        this.sendCount = sendCount;
    }

    @Override
    protected void editMsg(PoseStamped msg) {
       ;
        msg.getHeader().setStamp( new org.ros.message.Time((double)System.currentTimeMillis()/1000));

        msg.getHeader().setFrameId("map");
        msg.getPose().getPosition().setZ(0);
        msg.getPose().getPosition().setX(x);
        msg.getPose().getPosition().setY(y);
        msg.getPose().getOrientation().setW(w);
        msg.getPose().getOrientation().setZ(z);
        msg.getPose().getOrientation().setX(0);
        msg.getPose().getOrientation().setY(0);
    }
}
