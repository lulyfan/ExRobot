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

import java.io.File;

/**
 * Created by Administrator on 2017/10/30/030.
 */

public class MoveFragment extends Fragment{
    private String tip;
    private TextView tv_tip;
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
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        tv_tip.setText(tip);
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
}
