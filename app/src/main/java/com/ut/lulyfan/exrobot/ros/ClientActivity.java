package com.ut.lulyfan.exrobot.ros;

import android.os.Bundle;

import com.ut.lulyfan.exrobot.R;

import org.ros.android.RosActivity;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import std_msgs.Bool;
import std_msgs.Float64;
import std_msgs.Int64;

public class ClientActivity extends RosActivity {

    protected GoTalker goTalker;
    protected InitPoseTalker initPoseTalker;
    protected StringTalker switchMode;
    protected Listener<Bool> arriveListener;
    protected Listener<Int64> errorCode;
    protected Listener<Float64> battery;
    protected Listener<Bool> zwj_standby;

    public ClientActivity() {
        super("ExRobot", "ExRobot", URI.create("http://192.168.168.100:11311"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        goTalker = new GoTalker("move_base_simple/goal");
        initPoseTalker = new InitPoseTalker("initialpose");
        switchMode = new StringTalker("work_model");
        arriveListener = new Listener<>("Local/Goal_reached", Bool._TYPE);
        errorCode = new Listener<>("/error", Int64._TYPE);
        battery = new Listener<>("/battery", Float64._TYPE);
        zwj_standby = new Listener<>("standby/u_xiao_mei_go_go", Bool._TYPE);
    }


    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());


        arriveListener.setMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                if (bool.getData()) {
                    System.out.println("receive arrive msg");
                    if (arriveHandler != null) {
                        arriveHandler.hanldArrive();
                    }
                }
            }
        });

        errorCode.setMessageListener(new MessageListener<Int64>() {
            @Override
            public void onNewMessage(Int64 int64) {
                int code = (int) int64.getData();

                if (code == 3 && blockHandler != null) {
                    blockHandler.hanldBlock();
                }
            }
        });

        battery.setMessageListener(new MessageListener<Float64>() {
            @Override
            public void onNewMessage(Float64 float64) {
                handleBattery(float64.getData());
            }
        });

        zwj_standby.setMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                if (bool.getData()) {
                    handleStandby();
                }
            }
        });

        nodeMainExecutor.execute(goTalker, nodeConfiguration);
        nodeMainExecutor.execute(switchMode, nodeConfiguration);
        nodeMainExecutor.execute(initPoseTalker, nodeConfiguration);
        nodeMainExecutor.execute(arriveListener, nodeConfiguration);
        nodeMainExecutor.execute(errorCode, nodeConfiguration);
        nodeMainExecutor.execute(battery, nodeConfiguration);
        nodeMainExecutor.execute(zwj_standby, nodeConfiguration);

        afterRosInit();
    }

    protected void handleStandby() {
    }

    protected void afterRosInit() {

    }

    protected void handleBattery(double battery) {

    }

    private ArriveHandler arriveHandler;

    protected void setArriveHandler(ArriveHandler arriveHandler) {
        this.arriveHandler = arriveHandler;
    }

    protected interface ArriveHandler {
        void hanldArrive();
    }

    private BlockHandler blockHandler;

    protected void setBlockHandler(BlockHandler blockHandler) {
        this.blockHandler = blockHandler;
    }

    protected interface BlockHandler {
        void hanldBlock();
    }
}
