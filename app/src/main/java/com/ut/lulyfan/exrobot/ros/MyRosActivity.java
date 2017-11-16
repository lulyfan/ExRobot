package com.ut.lulyfan.exrobot.ros;

import android.os.Bundle;

import com.ut.lulyfan.exrobot.R;

import org.ros.android.RosActivity;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;
import std_msgs.Bool;

public class MyRosActivity extends RosActivity {

    Executor executor = Executors.newSingleThreadExecutor();
    protected GoTalker goTalker;
    protected InitPoseTalker initPoseTalker;
    protected StringTalker switchMode;
    protected Listener<Bool> arriveListener;

    public MyRosActivity() {
        super("ExRobot", "ExRobot", URI.create("http://localhost:11311"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ros);

    }

    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        nodeMainExecutorService.startMaster(false);
                    }
                });

                System.out.println("after startMaster");

                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
                nodeConfiguration.setMasterUri(getMasterUri());

                Listener<PoseStamped> goListener = new Listener<>("fan", PoseStamped._TYPE);
                goListener.setMessageListener(new MessageListener<PoseStamped>() {
                    @Override
                    public void onNewMessage(PoseStamped poseStamped) {
                        System.out.println(poseStamped.getPose());
                    }
                });

                Listener<PoseWithCovarianceStamped> initListener = new Listener("hello", PoseWithCovarianceStamped._TYPE);
                initListener.setMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
                    @Override
                    public void onNewMessage(PoseWithCovarianceStamped poseWithCovarianceStamped) {
                        System.out.println(poseWithCovarianceStamped);
                    }
                });

                arriveListener = new Listener<>("Local/Goal_reached", Bool._TYPE);

                goTalker = new GoTalker("fan");
                initPoseTalker = new InitPoseTalker("hello");
                switchMode = new StringTalker("work_model");

                nodeMainExecutor.execute(goListener, nodeConfiguration);
                nodeMainExecutor.execute(goTalker, nodeConfiguration);
                nodeMainExecutor.execute(switchMode, nodeConfiguration);
                nodeMainExecutor.execute(initListener, nodeConfiguration);
                nodeMainExecutor.execute(initPoseTalker, nodeConfiguration);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("start sendmsg...");
                goTalker.sendMsg(1, 1, 1, 1, 5, 1000);
                initPoseTalker.sendMsg(2, 2, 2, 2, 5, 1000);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nodeMainExecutorService.shutdown();
    }
}
