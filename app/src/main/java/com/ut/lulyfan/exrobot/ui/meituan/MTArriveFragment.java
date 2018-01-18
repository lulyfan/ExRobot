package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTArriveFragment extends Fragment{

    private static final String KEY = "custom";
    private SpeechSynthesizeManager ssm;

    static public Fragment newInstance(Customer customer) {
        Fragment fragment = new MTArriveFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, customer);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("您的外卖来啦，请点击取餐");

        View root = inflater.inflate(R.layout.mt_arrive_layout, container, false);
        final Customer customer = (Customer) getArguments().getSerializable(KEY);
        String phoneNum = customer.getPhoneNum();
        int length = phoneNum.length();
        String end4Num = phoneNum.substring(length - 4);

        TextView robotID = (TextView) root.findViewById(R.id.robotID);
        TextView info = (TextView) root.findViewById(R.id.info);
        Button button = (Button) root.findViewById(R.id.button);

        robotID.setText("No. " + customer.getRobotID());
        info.setText("手机尾号" + end4Num + "的用户\n您的外卖到了，请点击取餐");
        button.setText("取餐");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  MTGetFoodFragment.newInstance(customer));
                ft.commit();
            }
        });

        return root;
    }
}
