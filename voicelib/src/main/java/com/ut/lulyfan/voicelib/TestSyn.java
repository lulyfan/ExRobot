package com.ut.lulyfan.voicelib;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

public class TestSyn extends AppCompatActivity {

    SpeechSynthesizeManager speechSynthesizeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_syn);

        speechSynthesizeManager = new SpeechSynthesizeManager(this, false, handler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                speechSynthesizeManager.startSpeakingLoop("从前有座山，山里有个庙，庙里有个老和尚，给小和尚讲故事,讲的是:", 1000);
            }
        }, 3000);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechSynthesizeManager != null)
            speechSynthesizeManager.destory();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
}
