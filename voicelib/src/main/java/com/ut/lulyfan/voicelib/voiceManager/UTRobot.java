package com.ut.lulyfan.voicelib.voiceManager;

/**
 * Created by acer on 2016/12/13.
 */

public class UTRobot {

    private String sn;
    private String floor;    //楼层
    private String scene;   //场景，指机器人放置在哪个地点

    public UTRobot(String sn, String floor, String scene) {
        this.sn = sn;
        this.floor = floor;
        this.scene = scene;
    }

    public UTRobot() {
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }
}
