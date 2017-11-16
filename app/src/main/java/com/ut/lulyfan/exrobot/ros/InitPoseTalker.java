package com.ut.lulyfan.exrobot.ros;

import geometry_msgs.PoseWithCovarianceStamped;

/**
 * Created by Administrator on 2017/10/26/026.
 */

public class InitPoseTalker extends Talker<PoseWithCovarianceStamped>{

    private double x;
    private double y;
    private double w;
    private double z;

    public InitPoseTalker(String topic) {
        super(topic, PoseWithCovarianceStamped._TYPE);
    }

    public void sendMsg(double x, double y, double z, double w) {
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
    protected void editMsg(PoseWithCovarianceStamped msg) {
        msg.getHeader().setFrameId("map");
        msg.getHeader().setStamp(new org.ros.message.Time((double)System.currentTimeMillis()/1000));
        msg.getPose().getPose().getPosition().setZ(0);
        msg.getPose().getPose().getPosition().setX(x);
        msg.getPose().getPose().getPosition().setY(y);

        msg.getPose().getPose().getOrientation().setX(0);
        msg.getPose().getPose().getOrientation().setY(0);
        msg.getPose().getPose().getOrientation().setZ(z);
        msg.getPose().getPose().getOrientation().setW(w);
        msg.getPose().setCovariance(new double[36]);
    }
}
