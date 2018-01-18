package com.ut.lulyfan.exrobot.ui.meituan;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.ui.ExActivity;
import com.ut.lulyfan.exrobot.util.CustomerDBUtil;
import com.ut.lulyfan.exrobot.util.DoorUtil;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import java.util.List;

import greenDao.CustomerDao;

/**
 * Created by Administrator on 2017/12/28/028.
 */

public class MTInputFragment extends Fragment{

    private EditText et_phoneNum;
    private RecyclerView recyclerView;
    private Button bt_confirm;
    private Button bt_cancel;
    private Button bt_del;
    private DoorUtil doorUtil;
    private SpeechSynthesizeManager ssm;
    private Handler handler;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        doorUtil = new DoorUtil(getActivity());
        ssm = ((ExActivity) getActivity()).ssm;
        ssm.startSpeaking("请输入取餐用户手机号码打开餐箱");

        handler = ((ExActivity) getActivity()).handler;

        View root = inflater.inflate(R.layout.mt_input_layout, container, false);
        initUI(root);
        return root;
    }

    private void initUI(View view) {

        et_phoneNum = (EditText) view.findViewById(R.id.phoneNum);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        bt_confirm = (Button) view.findViewById(R.id.positive);
        bt_cancel = (Button) view.findViewById(R.id.negative);
        bt_del = (Button) view.findViewById(R.id.del);

        GridLayoutManager manager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(manager);
        KeyboardAdapter keyboardAdapter = new KeyboardAdapter(getActivity());
        keyboardAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String str = button.getText().toString();
                try {
                    Integer.parseInt(str);
                    et_phoneNum.append(str);
                } catch (NumberFormatException e) {

                }
            }
        });
        recyclerView.setAdapter(keyboardAdapter);

        bt_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int length = et_phoneNum.getText().length();
                if (length >= 1) {
                    et_phoneNum.getText().delete(length - 1, length);
                }
            }
        });

        bt_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phoneNum = et_phoneNum.getText().toString();

                if ("".equals(phoneNum)) {
                    return;
                }

                if (phoneNum.equals(getResources().getString(R.string.default_password))) {
                    doorUtil.open();
                    return;
                }

                CustomerDBUtil dbUtil = CustomerDBUtil.getInstance();
                List<Customer> customerList = dbUtil.queryCustomer(CustomerDao.Properties.PhoneNum, phoneNum);
                if (customerList != null && !customerList.isEmpty()) {
                    Customer customer = customerList.get(0);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.container,  MTFindedFragment.newInstance(customer));
                    ft.commit();

                    if (doorUtil.check() == DoorUtil.CLOSED) {
                        doorUtil.open();
                    }
                }
                else {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.container,  new MTNotFindFragment());
                    ft.commit();
                }
            }
        });

        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  new MTStartFragment());
                ft.commit();
            }
        });
    }

    public static class KeyboardAdapter extends RecyclerView.Adapter<KeyboardAdapter.MyViewHolder> {
        Context context;
        View.OnClickListener onClickListener;

        public KeyboardAdapter(Context context) {
            this.context = context;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.keyboard_item, null);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {

            int p = position + 1;
            if (p == 10) {
                holder.button.setText("*");
            } else if (p == 11) {
                holder.button.setText("0");
            } else if (p == 12) {
                holder.button.setText("#");
            } else {
                holder.button.setText(p + "");
            }

            holder.button.setOnClickListener(onClickListener);
        }

        @Override
        public int getItemCount() {
            return 12;
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            Button button;

            public MyViewHolder(View itemView) {
                super(itemView);
                button = (Button) itemView.findViewById(R.id.keyboard_item);
            }
        }
    }
}
