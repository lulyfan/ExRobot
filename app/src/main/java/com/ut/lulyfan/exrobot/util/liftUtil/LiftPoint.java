package com.ut.lulyfan.exrobot.util.liftUtil;

/**
 * Created by Administrator on 2018/1/16/016.
 */

public class LiftPoint {
    private int floor;
    public double[] inPoint = new double[4];    //电梯内点
    public double[] outPoint = new double[4];   //电梯外点
    public double[] initPoint = new double[4];

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }
}
