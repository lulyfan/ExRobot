package com.ut.lulyfan.voicelib;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.ut.lulyfan.voicelib.voiceManager.DongniVoiceHelper;
import com.ut.lulyfan.voicelib.voiceManager.UTRobot;

public class TestVoiceActivity extends AppCompatActivity {

    DongniVoiceHelper voiceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testvoice_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        FrameLayout voiceContainer = (FrameLayout) findViewById(R.id.voiceContainer);
        UTRobot utRobot = new UTRobot("robot-test", "4", "GMO.GMO.0755.01");

        if (DongniVoiceHelper.isHasInstance()) {
            voiceHelper = DongniVoiceHelper.getInstance();
            voiceHelper.setVoiceViewAndHandle(this, voiceContainer, handler);
        }
        else {
            voiceHelper = DongniVoiceHelper.getInstance(this, handler, voiceContainer, utRobot);
        }

        voiceHelper.initVoice();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (voiceHelper != null) {
            voiceHelper.pauseVoice();
        }
        voiceHelper = null;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
}
