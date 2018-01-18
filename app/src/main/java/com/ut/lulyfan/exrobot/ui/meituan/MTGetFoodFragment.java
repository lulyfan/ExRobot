package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import static com.ut.lulyfan.exrobot.ui.ExActivity.TASK_END;

/**
 * Created by Administrator on 2018/1/2/002.
 */

public class MTGetFoodFragment extends Fragment{
    private EditText et_code;
    private RecyclerView recyclerView;
    private Button bt_open;
    private Button bt_back;
    private Button bt_del;

    private String code;
    private SpeechSynthesizeManager ssm;
    private Handler handler;
    private Customer customer;

    private static final String KEY = "custom";

    static public Fragment newInstance(Customer customer) {
        Fragment fragment = new MTGetFoodFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, customer);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("输入您的4位取餐密码");
        handler = ((ExActivity) getActivity()).handler;

        customer = (Customer) getArguments().getSerializable(KEY);
        code = customer.getCode();

        View root = inflater.inflate(R.layout.mt_getfood_layout, container, false);
        initUI(root);
        return root;
    }

    private void initUI(View view) {
        et_code = (EditText) view.findViewById(R.id.code);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        bt_open = (Button) view.findViewById(R.id.open);
        bt_back = (Button) view.findViewById(R.id.back);
        bt_del = (Button) view.findViewById(R.id.del);

        GridLayoutManager manager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(manager);
        MTInputFragment.KeyboardAdapter keyboardAdapter = new MTInputFragment.KeyboardAdapter(getActivity());
        keyboardAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String str = button.getText().toString();
                try {
                    Integer.parseInt(str);
                    et_code.append(str);
                } catch (NumberFormatException e) {

                }
            }
        });
        recyclerView.setAdapter(keyboardAdapter);

        bt_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int length = et_code.getText().length();
                if (length >= 1) {
                    et_code.getText().delete(length - 1, length);
                }
            }
        });

        bt_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String input = et_code.getText().toString();

                if ("".equals(input)) {
                    return;
                }

                if (!input.equals(code) && !input.equals(getResources().getString(R.string.default_password))) {
                    et_code.setText("");
                    Toast.makeText(getActivity(), "取货码错误，请重新输入", Toast.LENGTH_SHORT).show();
                    return;
                }

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  new MTEndServiceFragment());
                ft.commit();
            }
        });

        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message.obtain(handler, TASK_END, customer).sendToTarget();
            }
        });
    }
}
