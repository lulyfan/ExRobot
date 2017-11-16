package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;

import static com.ut.lulyfan.exrobot.ui.ExActivity.END_EX;

/**
 * Created by Administrator on 2017/11/3/003.
 */

public class ShowResultFragment extends Fragment {
    private static final String KEY = "customer";
    private boolean isConfirm = false;  //客户是否按下确认领取快递按钮

    public static ShowResultFragment newInstance(Customer customer) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, customer);
        ShowResultFragment fragment = new ShowResultFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final Customer customer = (Customer) getArguments().getSerializable(KEY);

        View root = inflater.inflate(R.layout.fragment_show_result, container, false);
        TextView showResult = (TextView) root.findViewById(R.id.tv_showResult);
        showResult.setText(customer.getName() + ",您有"+customer.getExCount()+"件快递到了,领取后请点击确认领取");

        final Handler handler = ((ExActivity)getActivity()).handler;

        //点击确认领取快递按钮，发送快递结束消息
        final Button bt_confirm = (Button) root.findViewById(R.id.bt_confirm);

        bt_confirm.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        bt_confirm.setBackgroundColor(Color.LTGRAY);
                        break;
                    case MotionEvent.ACTION_UP:

                        isConfirm = true;
                        handler.sendEmptyMessage(END_EX);
                        break;
                    default:
                }
                return true;
            }
        });

        //如果用户在超时时间内未确认领取快递，发送快递结束消息
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isConfirm)
                    Message.obtain(handler, END_EX, customer).sendToTarget();
            }
        }, 2 * 60 * 1000);

        return root;
    }
}
