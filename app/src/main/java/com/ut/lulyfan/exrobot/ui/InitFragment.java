package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ut.lulyfan.exrobot.R;

/**
 * Created by Administrator on 2017/11/14/014.
 */

public class InitFragment extends Fragment{
    Button start;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_init, container, false);
        start = (Button) root.findViewById(R.id.bt_start);
        start.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        start.setBackgroundResource(R.drawable.bt_bg_pressed);
                        start.setTextColor(Color.WHITE);
                        break;
                    case MotionEvent.ACTION_UP:
                        Handler handler = ((ExActivity)getActivity()).handler;
                        handler.sendEmptyMessage(ExActivity.INIT);
                        break;
                }
                return true;
            }
        });
        return root;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void ready() {
        start.setEnabled(true);
        start.setBackgroundResource(R.drawable.bt_bg_enable);
        start.setTextColor(0xFFD943AE);
    }
}
