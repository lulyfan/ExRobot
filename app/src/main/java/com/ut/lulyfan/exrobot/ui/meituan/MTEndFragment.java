package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTEndFragment extends Fragment{
    private SpeechSynthesizeManager ssm;
    private Handler handler;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("祝您用餐愉快");
        handler = ((ExActivity) getActivity()).handler;
        handler.sendEmptyMessageDelayed(ExActivity.TASK_END, 3000);

        View root = inflater.inflate(R.layout.mt_end_layout, container, false);
        return root;
    }
}
