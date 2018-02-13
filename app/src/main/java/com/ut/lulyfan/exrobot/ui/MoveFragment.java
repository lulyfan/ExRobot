package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.util.liftUtil.LiftControl;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import java.io.File;

/**
 * Created by Administrator on 2017/10/30/030.
 */

public class MoveFragment extends Fragment implements LiftControl.StateListener{
    private String tip;
    private TextView tv_tip;
    private static final String KEY = "tip";
    private SpeechSynthesizeManager ssm;

    public static Fragment newInstance(String tip) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY, tip);
        MoveFragment fragment = new MoveFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_move, container, false);
        WebView webView = (WebView) root.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);  //设置 缓存模式
        // 开启 DOM storage API 功能
        webView.getSettings().setDomStorageEnabled(true);
        //开启 database storage API 功能
        webView.getSettings().setDatabaseEnabled(true);
        String cacheDirPath = "/sdcard/UTRobot/webCache";

        File file = new File(cacheDirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        //设置  Application Caches 缓存目录
        webView.getSettings().setAppCachePath(cacheDirPath);
        //开启 Application Caches 功能
        webView.getSettings().setAppCacheEnabled(true);
        webView.loadUrl("file:///android_asset/test.html");

        tv_tip = (TextView) root.findViewById(R.id.tv_tip);

        if (getArguments() != null) {
            tip = getArguments().getString(KEY);
        }

        ssm = ((ExActivity)getActivity()).ssm;

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        tv_tip.setText(tip);
    }

    @Override
    public void callCurFloor(final int curFloor) {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("正在呼叫" + curFloor + "楼电梯...");
                }
            });
        }
    }

    @Override
    public void goIn() {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("正在进入电梯...");
                }
            });
        }
        ssm.startSpeaking("我要进梯了,请让一让");
    }

    @Override
    public void goOut() {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("正在出梯...");
                }
            });
        }
        ssm.startSpeaking("我要出梯了,请让一让");
    }

    @Override
    public void goInFailed() {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("入梯失败");
                }
            });
        }

    }

    @Override
    public void goOutFailed() {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("出梯失败");
                }
            });
        }

    }

    @Override
    public void callDstFloor(final int dstFloor) {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("正在呼叫" + dstFloor + "楼电梯...");
                }
            });
        }

    }

    @Override
    public void goTaskPoint() {
        if (tv_tip != null) {
            tv_tip.post(new Runnable() {
                @Override
                public void run() {
                    tv_tip.setText("开始派送任务");
                }
            });
        }
    }
}
