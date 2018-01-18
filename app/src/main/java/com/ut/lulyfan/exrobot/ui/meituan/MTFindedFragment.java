package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.exrobot.util.DoorUtil;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTFindedFragment extends Fragment{

    private static final String KEY = "custom";
    private Handler handler;
    private SpeechSynthesizeManager ssm;
    private DoorUtil doorUtil;

    static public Fragment newInstance(Customer customer) {
        Fragment fragment = new MTFindedFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, customer);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("关闭货箱并点击锁箱按钮完成放餐");
        doorUtil = new DoorUtil(getActivity());
        handler = ((ExActivity) getActivity()).handler;

        final Customer customer = (Customer) getArguments().getSerializable(KEY);
        String phoneNum = customer.getPhoneNum();

        View root = inflater.inflate(R.layout.mt_finded_layout, container, false);
        TextView textView = (TextView) root.findViewById(R.id.text1);
        textView.setText("确认用户手机号\n" + phoneNum);

        TextView reput = (TextView) root.findViewById(R.id.text2);
        reput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  new MTInputFragment());
                ft.commit();
            }
        });

        Button start = (Button) root.findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (doorUtil.check() == DoorUtil.CLOSED) {
                    List<Customer> customers = new ArrayList<Customer>();
                    customers.add(customer);
                    Message.obtain(handler, ExActivity.TASK_START, customers).sendToTarget();
                } else {
                    ssm.startSpeaking("请先关闭货箱");
                }
            }
        });
        return root;
    }
}
