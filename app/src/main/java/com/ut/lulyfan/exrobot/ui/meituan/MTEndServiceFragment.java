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
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.exrobot.util.DoorUtil;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTEndServiceFragment extends Fragment{
    private SpeechSynthesizeManager ssm;
    private DoorUtil doorUtil;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        doorUtil = new DoorUtil(getActivity());
        doorUtil.open();
        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("请您全部取出您的订餐，以免遗漏；取出后关闭货箱，点击结束服务");

        View root = inflater.inflate(R.layout.mt_layout, container, false);
        TextView textView = (TextView) root.findViewById(R.id.info);
        Button button = (Button) root.findViewById(R.id.button);
        button.setBackgroundResource(R.drawable.bt_red_select);
        TextView textView1 = (TextView) root.findViewById(R.id.tail);

        textView.setText("请您取出餐品后关闭货箱\n并点击屏幕下方的结束服务");
        textView1.setText("风险提示：结束服务后将不能再次打开餐箱");
        textView1.setVisibility(View.VISIBLE);
        button.setText("结束服务");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (doorUtil.check() == DoorUtil.CLOSED) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.container, new MTEndFragment());
                    ft.commit();
                } else {
                    ssm.startSpeaking("请先关闭货箱，再点击结束服务");
                }
            }
        });
        return root;
    }
}
