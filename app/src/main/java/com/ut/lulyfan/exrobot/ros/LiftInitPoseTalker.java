package com.ut.lulyfan.exrobot.ros;


import custom_msgs_srvs.InitialFloorPose;

/**
 * Created by Administrator on 2018/1/8/008.
 */

public class LiftInitPoseTalker extends Talker<InitialFloorPose>{

    private double x;
    private double y;
    private double w;
    private double z;
    private int floor;

    public LiftInitPoseTalker(String topic) {
        super(topic, InitialFloorPose._TYPE);
    }

    public void sendMsg(int floor, double x, double y, double z, double w) {
        sendMsg(floor, x, y, w, z, 1);
    }

    public void sendMsg(int floor, double x, double y, double z, double w, int sendCount) {
        sendMsg(floor, x, y, w, z, sendCount, 1000);
    }

    public void sendMsg(int floor, double x, double y, double z, double w, int sendCount, long gapTime) {

        this.x = x;
        this.y = y;
        this.w = w;
        this.z = z;
        this.floor = floor;
        this.gapTime = gapTime;
        this.sendCount = sendCount;
    }

    @Override
    protected void editMsg(InitialFloorPose msg) {
        msg.getPose().getHeader().setFrameId("map");
        msg.getPose().getHeader().setStamp(new org.ros.message.Time((double)System.currentTimeMillis()/1000));
        msg.getPose().getPose().getPose().getPosition().setZ(0);
        msg.getPose().getPose().getPose().getPosition().setX(x);
        msg.getPose().getPose().getPose().getPosition().setY(y);

        msg.getPose().getPose().getPose().getOrientation().setX(0);
        msg.getPose().getPose().getPose().getOrientation().setY(0);
        msg.getPose().getPose().getPose().getOrientation().setZ(z);
        msg.getPose().getPose().getPose().getOrientation().setW(w);
        msg.getPose().getPose().setCovariance(new double[36]);

        msg.setFloorId(floor);
    }
}
