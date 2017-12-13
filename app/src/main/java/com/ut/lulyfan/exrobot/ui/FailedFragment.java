package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.util.DoorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/11/10/010.
 */

public class FailedFragment extends Fragment{

    private static final String KEY = "failedCustomer";
    private DoorUtil doorUtil;

    public static FailedFragment getInstance(ArrayList<Customer> customers) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, customers);
        FailedFragment fragment = new FailedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        doorUtil = new DoorUtil(getActivity());

        List<Customer> customers = (List<Customer>) getArguments().getSerializable(KEY);
        final Handler handler = ((ExActivity)getActivity()).handler;

        View root = inflater.inflate(R.layout.fragment_failed, container, false);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.FailedExList);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(new ExListAdapter(getActivity(), customers));

        final Button goHome = (Button) root.findViewById(R.id.goHome);
        goHome.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        goHome.setBackgroundColor(Color.LTGRAY);
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.sendEmptyMessage(ExActivity.GO_HOME);
                        break;
                    default:
                }
                return true;
            }
        });

        final Button open = (Button) root.findViewById(R.id.open);
        open.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        open.setBackgroundColor(Color.LTGRAY);
                        break;
                    case MotionEvent.ACTION_UP:
                        open.setBackgroundColor(Color.TRANSPARENT);
                        doorUtil.open();
                        break;
                    default:
                }
                return true;
            }
        });
        return root;
    }


}
