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

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTNotFindFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.mt_layout, container, false);
        TextView textView = (TextView) root.findViewById(R.id.info);
        Button button = (Button) root.findViewById(R.id.button);

        textView.setText("该用户不在配送名单内");
        button.setText("重新输入");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  new MTInputFragment());
                ft.commit();
            }
        });
        return root;
    }
}
