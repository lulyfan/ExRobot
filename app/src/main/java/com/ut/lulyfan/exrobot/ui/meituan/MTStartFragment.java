package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ut.lulyfan.exrobot.R;

/**
 * Created by Administrator on 2017/12/28/028.
 */

public class MTStartFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.mt_start_layout, container, false);
        Button bt_put = (Button) root.findViewById(R.id.put);
        bt_put.setOnClickListener(new View.OnClickListener() {
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
