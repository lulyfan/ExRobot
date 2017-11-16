package com.ut.lulyfan.exrobot.ros;

import android.os.Bundle;
import android.view.View;

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

public class MasterActivity extends RosActivity {

    Executor executor = Executors.newSingleThreadExecutor();
    protected BoolTalker arriveTalker;

    public MasterActivity() {
        super("ExRobot", "ExRobot", URI.create("http://localhost:11311"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        arriveTalker = new BoolTalker("Local/Goal_reached");
    }

    public void onClick(View view) {
        if (view.getId() == R.id.sendArrive) {
            arriveTalker.sendMsg(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nodeMainExecutorService.shutdown();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                nodeMainExecutorService.startMaster(false);
            }
        });

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());

        Listener<PoseStamped> goListener = new Listener<>("move_base_simple/goal", PoseStamped._TYPE);
        goListener.setMessageListener(new MessageListener<PoseStamped>() {
            @Override
            public void onNewMessage(PoseStamped poseStamped) {
                System.out.println(poseStamped.getPose());
            }
        });

        Listener<PoseWithCovarianceStamped> initListener = new Listener("initialpose", PoseWithCovarianceStamped._TYPE);
        initListener.setMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped poseWithCovarianceStamped) {
                System.out.println(poseWithCovarianceStamped);
            }
        });



        nodeMainExecutor.execute(goListener, nodeConfiguration);
        nodeMainExecutor.execute(initListener, nodeConfiguration);
        nodeMainExecutor.execute(arriveTalker, nodeConfiguration);
    }
}
