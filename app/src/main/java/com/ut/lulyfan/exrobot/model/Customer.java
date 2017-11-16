package com.ut.lulyfan.exrobot.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/10/30/030.
 */
@Entity
public class Customer implements Serializable{

    private static final long serialVersionUID = 0L;

    @Id(autoincrement = true)
    private Long id;

    private String name;

    private String phoneNum;

    private int floor;

    private String area;

    private double x;
    private double y;
    private double w;
    private double z;

    @Transient
    private int exCount = 1;

    @Generated(hash = 757791955)
    public Customer(Long id, String name, String phoneNum, int floor, String area,
            double x, double y, double w, double z) {
        this.id = id;
        this.name = name;
        this.phoneNum = phoneNum;
        this.floor = floor;
        this.area = area;
        this.x = x;
        this.y = y;
        this.w = w;
        this.z = z;
    }

    @Generated(hash = 60841032)
    public Customer() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getW() {
        return this.w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getArea() {
        return this.area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public int getExCount() {
        return exCount;
    }

    public void setExCount(int exCount) {
        this.exCount = exCount;
    }
}
