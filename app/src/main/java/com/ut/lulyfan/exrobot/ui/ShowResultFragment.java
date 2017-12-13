package com.ut.lulyfan.exrobot.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.omadahealth.lollipin.lib.enums.KeyboardButtonEnum;
import com.github.omadahealth.lollipin.lib.interfaces.KeyboardButtonClickedListener;
import com.github.omadahealth.lollipin.lib.views.KeyboardView;
import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.util.DoorUtil;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import static com.ut.lulyfan.exrobot.ui.ExActivity.END_EX;
import static com.ut.lulyfan.exrobot.util.DoorUtil.CLOSED;

/**
 * Created by Administrator on 2017/11/3/003.
 */

public class ShowResultFragment extends Fragment {
    private static final String KEY = "customer";
    private boolean isVerifySuccessed = false; //是否输入了正确的取货码
    private Handler handler;
    private SpeechSynthesizeManager ssm;
    private DoorUtil doorUtil;
    private Customer customer;

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

        doorUtil = new DoorUtil(getActivity());

        customer = (Customer) getArguments().getSerializable(KEY);
        final String code = customer.getCode();

        View root = inflater.inflate(R.layout.fragment_show_result, container, false);
        final TextView showResult = (TextView) root.findViewById(R.id.tv_showResult);
        showResult.setText(customer.getName() + ",您有"+customer.getExCount()+"件快递到了");
        TextView countdown = (TextView) root.findViewById(R.id.countdown);

        handler = ((ExActivity)getActivity()).handler;
        ssm = ((ExActivity)getActivity()).ssm;

        final EditText tv_password = (EditText) root.findViewById(R.id.password);
        tv_password.setFocusable(false);

        final CountdownTask countdownTask = new CountdownTask(countdown);
        countdownTask.execute(120);

        final KeyboardView keyboardView = (KeyboardView) root.findViewById(R.id.keyboard_view);
        ((TextView)(keyboardView.findViewById(R.id.pin_code_button_sure).findViewById(R.id.keyboard_button_textview))).setText("确认");
        keyboardView.setKeyboardButtonClickedListener(new KeyboardButtonClickedListener() {
            @Override
            public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum) {
                switch (keyboardButtonEnum) {

                    case BUTTON_CLEAR:
                        int length = tv_password.getText().length();
                        if (length > 0) {
                            String text = tv_password.getText().subSequence(0, length - 1).toString();
                            tv_password.setText(text);
                        }
                        break;

                    case BUTTON_SURE:
                        String input = tv_password.getText().toString();

                        if (input.equals("")) {
                            Toast.makeText(getActivity(), "请先输入密码", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!input.equals(code) && !input.equals(getResources().getString(R.string.default_password))) {
                            tv_password.setText("");
                            Toast.makeText(getActivity(), "取货码错误，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        isVerifySuccessed = true;
                        keyboardView.setFocusable(false);
                        showResult.setText("请领取快递,并关闭厢门");
                        countdownTask.cancel(true);
                        ((ExActivity)getActivity()).executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                getEx();
                            }
                        });
                        break;

                    default:
                        tv_password.append(keyboardButtonEnum.getButtonValue() + "");
                }
            }

            @Override
            public void onRippleAnimationEnd() {

            }
        });



        //如果用户在超时时间内未输入正确的取货码，发送快递结束消息
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (!isVerifySuccessed)
//                    Message.obtain(handler, END_EX, customer).sendToTarget();
//            }
//        }, 2 * 60 * 1000);

        return root;
    }

    private void getEx() {

        doorUtil.open();
        ssm.startSpeakingLoop("请领取快递，并厢门关闭", 20 * 1000);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {

            if (doorUtil.check() == CLOSED) {
                ssm.stopSpeakingLoop();
                handler.sendEmptyMessage(ExActivity.END_EX);
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    static class AsyncTask<Inteter> extends android.os.AsyncTask {
//
//        @Override
//        protected void onProgressUpdate(Object[] values) {
//            countdown.setText(values[0] + "S");
//        }
//
//        @Override
//        protected void onPostExecute(Object o) {
//            Message.obtain(handler, END_EX, customer).sendToTarget();
//        }
//
//        @Override
//        protected java.lang.Object doInBackground(java.lang.Object[] params) {
//            int seconds = (int) params[0];
//            while (seconds > 0) {
//
//                if (isCancelled())
//                    break;
//
//                publishProgress(seconds);
//                try {
//                    Thread.sleep(1000);
//                    seconds --;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            return null;
//        }
//    }

    class CountdownTask extends AsyncTask<Integer, Integer, Object> {

        TextView countdown;

        public CountdownTask(TextView textView) {
            countdown = textView;
        }

        @Override
        protected Object doInBackground(Integer... params) {
            int seconds = params[0];
            while (seconds > 0) {

                if (isCancelled())
                    break;

                publishProgress(seconds);
                try {
                    Thread.sleep(1000);
                    seconds --;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            countdown.setText(values[0] + "");
        }

        @Override
        protected void onPostExecute(Object o) {
            Message.obtain(handler, END_EX, customer).sendToTarget();
        }
    }
}
