package com.ut.lulyfan.exrobot.ros;

import custom_msgs_srvs.ElevatorSendGoal;

/**
 * Created by Administrator on 2018/1/22/022.
 */

public class NewGoTalker extends Talker<ElevatorSendGoal> {

    private double x;
    private double y;
    private double w;
    private double z;
    private int mode;

    public static final int NORMAL_MODE = 0;
    public static final int LIFT_GOIN_MODE = 1;   //进电梯模式
    public static final int LIFT_GOOUT_MODE = 2;  //出电梯模式

    public NewGoTalker(String topic) {
        super(topic, ElevatorSendGoal._TYPE);
    }

    public void sendMsg(int mode, double x, double y, double z, double w ) {
        sendMsg(mode, x, y, w, z, 1);
    }

    public void sendMsg(int mode, double x, double y, double z, double w, int sendCount) {
        sendMsg(mode, x, y, w, z, sendCount, 1000);
    }

    public void sendMsg(int mode, double x, double y, double z, double w, int sendCount, long gapTime) {

        this.mode = mode;
        this.x = x;
        this.y = y;
        this.w = w;
        this.z = z;
        this.gapTime = gapTime;
        this.sendCount = sendCount;
    }


    @Override
    protected void editMsg(ElevatorSendGoal msg) {
        msg.setMode((byte) mode);

        msg.getGoal().getHeader().setStamp( new org.ros.message.Time((double)System.currentTimeMillis()/1000));
        msg.getGoal().getHeader().setFrameId("map");
        msg.getGoal().getPose().getPosition().setZ(0);
        msg.getGoal().getPose().getPosition().setX(x);
        msg.getGoal().getPose().getPosition().setY(y);
        msg.getGoal().getPose().getOrientation().setW(w);
        msg.getGoal().getPose().getOrientation().setZ(z);
        msg.getGoal().getPose().getOrientation().setX(0);
        msg.getGoal().getPose().getOrientation().setY(0);
    }
}
