package com.ut.lulyfan.exrobot.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.ut.lulyfan.exrobot.R;

public class RootActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        if (SettingActivity.checkSetting(this)) {
            Intent intent = new Intent(this, ExActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
        }

        finish();
    }


}
