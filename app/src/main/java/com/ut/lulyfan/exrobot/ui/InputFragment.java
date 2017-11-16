package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.omadahealth.lollipin.lib.enums.KeyboardButtonEnum;
import com.github.omadahealth.lollipin.lib.interfaces.KeyboardButtonClickedListener;
import com.github.omadahealth.lollipin.lib.views.KeyboardView;
import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.util.CustomerDBUtil;

import java.util.List;

import greenDao.CustomerDao;

/**
 * Created by Administrator on 2017/11/3/003.
 */

//负责快递信息的输入
public class InputFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_init, container, false);
        final LinearLayout exContainer = (LinearLayout) root.findViewById(R.id.ExContainer);

        final RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.ExList);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new RecycleViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL, 3, Color.LTGRAY));
        recyclerView.setAdapter(new ExListAdapter(getActivity()));

        final TextView et_phoneNum = (TextView) root.findViewById(R.id.phoneNum);

        KeyboardView keyboardView = (KeyboardView) root.findViewById(R.id.keyboard_view);
        keyboardView.setKeyboardButtonClickedListener(new KeyboardButtonClickedListener() {
            @Override
            public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum) {
                switch (keyboardButtonEnum) {

                    case BUTTON_CLEAR:
                        int length = et_phoneNum.getText().length();
                        if (length > 0) {
                            String text = et_phoneNum.getText().subSequence(0, length - 1).toString();
                            et_phoneNum.setText(text);
                        }
                        break;

                    case BUTTON_SURE:
                        String phoneNum = et_phoneNum.getText().toString();

                        if (phoneNum.equals("")) {
                            Toast.makeText(getActivity(), "请先输入手机号", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<Customer> currentList = ((ExListAdapter) recyclerView.getAdapter()).getExList();
                        for (Customer customer : currentList) {
                            if (customer.getPhoneNum().equals(phoneNum)) {
//                                Toast.makeText(getActivity(), "from recyclerView", Toast.LENGTH_SHORT).show();
                                customer.setExCount(customer.getExCount() + 1);
                                ((ExListAdapter) recyclerView.getAdapter()).notifyDataSetChanged();
                                et_phoneNum.setText("");
                                return;
                            }
                        }

                        CustomerDBUtil dbUtil = CustomerDBUtil.getInstance();
                        List<Customer> customerList = dbUtil.queryCustomer(CustomerDao.Properties.PhoneNum, phoneNum);
                        if (customerList != null && !customerList.isEmpty()) {
//                            Toast.makeText(getActivity(), "from db", Toast.LENGTH_SHORT).show();
                            exContainer.setVisibility(View.VISIBLE);
                            Customer customer = customerList.get(0);
                            customer.setExCount(1);   //解决数据库缓存问题
                            ((ExListAdapter) recyclerView.getAdapter()).addEx(customer);
                            et_phoneNum.setText("");
                        }
                        else {
                            Toast.makeText(getActivity(), "找不到" + phoneNum + "相应信息,请重新输入", Toast.LENGTH_SHORT).show();
                            et_phoneNum.setText("");
                        }
                        break;

                    default:
                        et_phoneNum.append(keyboardButtonEnum.getButtonValue() + "");
                }
            }

            @Override
            public void onRippleAnimationEnd() {

            }
        });

        final Button startEx = (Button) root.findViewById(R.id.start);
        startEx.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startEx.setBackgroundColor(Color.LTGRAY);
                        break;

                    case MotionEvent.ACTION_UP:
                        startEx.setBackgroundColor(Color.TRANSPARENT);
//                        startEx.setTextColor(Color.GRAY);
//                        Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.start);
//                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
//                        startEx.setCompoundDrawables(drawable, null, null, null);

                        List<Customer> customers = ((ExListAdapter)recyclerView.getAdapter()).getExList();
                        if (customers.size() == 0)
                            Toast.makeText(getActivity(), "未添加快递任务", Toast.LENGTH_SHORT).show();
                        else {
                            Handler handler = ((ExActivity) getActivity()).handler;
                            Message.obtain(handler, ExActivity.START_EX, customers).sendToTarget();
                        }
                        break;
                    default:
                }
                return true;
            }
        });

        return root;
    }


}
