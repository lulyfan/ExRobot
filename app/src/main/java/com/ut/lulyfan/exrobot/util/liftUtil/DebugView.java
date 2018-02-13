package com.ut.lulyfan.exrobot.util.liftUtil;

import android.app.Activity;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;

/**
 * Created by Administrator on 2018/1/12/012.
 */

public class DebugView {
    PopupWindow popupWindow;
    TextView textView;
    Button close;
    Button clear;
    RelativeLayout headView;
    View root;
    int x;
    int y;
    float firstX;
    float firstY;

    static DebugView debugView;

    static public DebugView getInstance(final Context context) {
        if (debugView == null){
            debugView = new DebugView(context);
        }
        return debugView;
    }

    private DebugView(final Context context) {
        root = ((Activity)context).getWindow().getDecorView().getRootView();
        View view = LayoutInflater.from(context).inflate(R.layout.debug_layout, null);
        headView = (RelativeLayout) view.findViewById(R.id.head);
        headView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        firstX = event.getRawX();
                        firstY = event.getRawY();
                        System.out.println("x:" + x + " y:" + y);
                        return true;

                    case MotionEvent.ACTION_UP:

                        int moveX = (int) (event.getRawX() - firstX);
                        int moveY = (int) (event.getRawY() - firstY);
                        x = x - moveX;
                        y = y + moveY;
                        System.out.println("x:" + x + " y:" + y);

                        popupWindow.dismiss();
                        popupWindow.showAtLocation(root, Gravity.RIGHT | Gravity.TOP, x, y);

                        break;
                    default:

                }
                return false;
            }
        });
        textView = (TextView) view.findViewById(R.id.log);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        close = (Button) view.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        clear = (Button) view.findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
            }
        });
        popupWindow = new PopupWindow(view, 500, 500);
    }

    public void log(final String text) {
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.append(text);
            }
        });
    }

    public void showView() {
        popupWindow.showAtLocation(root, Gravity.RIGHT | Gravity.TOP, 100, 100);
    }

    public static void show() {
        if (debugView != null) {
            debugView.showView();
        }
    }

    public static void println(String text) {
        if (debugView != null) {
            debugView.log(text + "\n");
        }
    }

    public static void print(String text) {
        if (debugView != null) {
            debugView.log(text);
        }
    }

    public static void destory() {
        debugView = null;
    }
}
