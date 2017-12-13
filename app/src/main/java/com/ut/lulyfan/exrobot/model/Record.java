package com.ut.lulyfan.exrobot.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2017/12/7/007.
 */

public class Record {

    private String sn;
    private String task;
    private String description;
    private String time;
    private String location;

    public Record(String sn, String task, String location) {
        this.sn = sn;
        this.task = task;
        this.location = location;
        this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
