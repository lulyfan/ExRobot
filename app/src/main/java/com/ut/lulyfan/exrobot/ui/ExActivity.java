package com.ut.lulyfan.exrobot.ui;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;

import com.iflytek.cloud.SpeechError;
import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.debug.LogInFile;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.model.Record;
import com.ut.lulyfan.exrobot.ros.ClientActivity;
import com.ut.lulyfan.exrobot.ui.meituan.MTArriveFragment;
import com.ut.lulyfan.exrobot.ui.meituan.MTStartFragment;
import com.ut.lulyfan.exrobot.util.MySqlUtil;
import com.ut.lulyfan.exrobot.util.SmsUtil;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExActivity extends ClientActivity {

    public static final int TASK_START = 0;
    public static final int TASK_END = 1;
    public static final int GO_HOME = 2; //返回主界面
    public static final int INIT = 3;

    private static final int BlockSynTime = 2000;   //阻塞语音播放的间隔时间

    private List<Customer> customers = new ArrayList<>();  //快递的送达客户
    private List<Customer> curCustomers = new ArrayList<>();  //当前要送达的某个楼层的客户
    private ArrayList<Customer> failedCustomers = new ArrayList<>();  //快递未领取的客户
    private int initFloor ;  //机器人的初始化楼层
    private int curFloor ;      //机器人当前所处楼层
    private double[] initPosition = new double[4];
    private double[] exPosition = new double[4];    //获取快递的坐标点
    private long lastTime;   //检测长按的时间
    private long blockSynEndTime;     //阻塞时语音合成结束时间
    private MoveFragment moveFragment;
    private InitFragment initFragment;
    private BatteryView batteryView;
    public SpeechSynthesizeManager ssm;
    static Executor executor = Executors.newCachedThreadPool();
    public static String sn;
    public static String area;
    private List<Record> failedRecords = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ex);

        batteryView = (BatteryView) findViewById(R.id.battery);

        ssm = new SpeechSynthesizeManager(this, false, handler);
        moveFragment = new MoveFragment();
        initFragment = new InitFragment();

        setBlockHandler(new BlockHandler() {
            @Override
            public void hanldBlock() {
                if (!ssm.isInSession() && System.currentTimeMillis() - blockSynEndTime >= BlockSynTime) {
                    ssm.setSynCompletedListener(new SpeechSynthesizeManager.SynCompletedListener() {
                        @Override
                        public void onSynCompleted(SpeechError error) {
                            blockSynEndTime = System.currentTimeMillis();
                            ssm.setSynCompletedListener(null);
                        }
                    });
                    ssm.startSpeaking("请让一让，谢谢");
                }
            }
        });
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.i("ui", "activity onTouchEvent");
        long gapTime;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTime = System.currentTimeMillis();
            case MotionEvent.ACTION_UP:
                gapTime = System.currentTimeMillis() - lastTime;
                if (gapTime >= 5000) {
                    Intent intent = new Intent(this, SettingActivity.class);
                    startActivity(intent);
                }
                break;
            default:
        }
        return super.dispatchTouchEvent(ev);
    }

    private void getSetting() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String sSN = sharedPref.getString(SettingActivity.SettingsFragment.KEY_SN, null);
        String sfloor = sharedPref.getString(SettingActivity.SettingsFragment.KEY_FLOOR, "1");
        String sArea = sharedPref.getString(SettingActivity.SettingsFragment.KEY_AREA, "金地");
        String sInitPosition = sharedPref.getString(SettingActivity.SettingsFragment.KEY_INIT_POSITION, null);
        String sExPosition = sharedPref.getString(SettingActivity.SettingsFragment.KEY_EX_POSITION, null);

        sn = sSN;
        area = sArea;
        initFloor = Integer.parseInt(sfloor);

        String[] tmp = sInitPosition.split(",");
        initPosition[0] = Double.valueOf(tmp[0]);
        initPosition[1] = Double.valueOf(tmp[1]);
        initPosition[2] = Double.valueOf(tmp[2]);
        initPosition[3] = Double.valueOf(tmp[3]);

        tmp = sExPosition.split(",");
        exPosition[0] = Double.valueOf(tmp[0]);
        exPosition[1] = Double.valueOf(tmp[1]);
        exPosition[2] = Double.valueOf(tmp[2]);
        exPosition[3] = Double.valueOf(tmp[3]);
    }

    private boolean isInited;
    @Override
    protected void onStart() {
        super.onStart();
        getSetting();
        isInited = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container, initFragment);
        ft.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nodeMainExecutorService.forceShutdown();
        ssm.destory();
    }

    private long batteryUpdateTime;
    @Override
    protected void handleBattery(final double battery) {

        if (System.currentTimeMillis() - batteryUpdateTime < 5000) {
            return;
        }

        batteryUpdateTime = System.currentTimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                batteryView.setPower((int) battery);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void handleStandby() {
        if (!isInited) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    isInited = true;
                    initFragment.ready();
                }
            });
        }
    }

    //快递任务
    public void runTask(final Customer customer) {

        ssm.startSpeaking("开始派送"+customer.getName()+"的外卖");

        goTalker.sendMsg(customer.getX(), customer.getY(), customer.getW(), customer.getZ());
        //切换到移动的fragment
        moveFragment.setTip("正在派送"+customer.getName()+"的外卖...");
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container,  moveFragment);
        ft.commit();

        String nowTime = System.currentTimeMillis() + "";
        String code = nowTime.substring(nowTime.length() - 4);
        customer.setCode(code);
        SmsUtil.asyncSend(customer.getPhoneNum(), SmsUtil.MT_START, "{\"code\":\" "+ code + "\"}");

        setArriveHandler(new ArriveHandler() {
            @Override
            public void hanldArrive() {
                //切换到快递送达的结果fragment
                FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  MTArriveFragment.newInstance(customer));
                ft.commit();

                String code = customer.getCode();
                SmsUtil.asyncSend(customer.getPhoneNum(), SmsUtil.MT_ARRIVE, "{\"code\":\" "+ code + "\"}");

                Record record = new Record(sn, "迎宾", area);
                recordData(record);

                setArriveHandler(null);
            }
        });
    }

    //去取快递的地点（领取快递）
    public void goExpressPosition() {

        goTalker.sendMsg(exPosition[0], exPosition[1], exPosition[2], exPosition[3]);

        moveFragment.setTip("正在前往前台...");
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container, moveFragment);
        ft.commit();

        setArriveHandler(new ArriveHandler() {
            @Override
            public void hanldArrive() {
                //切换到快递送达的结果fragment
                FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
                ft.replace(R.id.container,  new MTStartFragment());
                ft.commit();

                setArriveHandler(null);
            }
        });
    }

    public void back() {

        goTalker.sendMsg(exPosition[0], exPosition[1], exPosition[2], exPosition[3]);

//        ssm.startSpeaking("快递派送完毕,开始返回...");
        moveFragment.setTip("外卖派送完毕,正在返回...");
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container, moveFragment);
        ft.commit();

        setArriveHandler(new ArriveHandler() {
            @Override
            public void hanldArrive() {
                //切换到快递送达的结果fragment
                FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
                if (failedCustomers.isEmpty()) {
                    ft.replace(R.id.container, new MTStartFragment());
                } else {
                    ssm.startSpeaking("有未签收外卖，请查看");
                    ft.replace(R.id.container, FailedFragment.getInstance(failedCustomers));
                }
                ft.commit();

                recordData(failedRecords);

                setArriveHandler(null);
            }
        });
    }

    private void recordData(final Record record) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MySqlUtil.insertData(record);
                } catch (SQLException e) {
                    if (failedRecords.size() >= 500) {
                        failedRecords.clear();
                    }
                    failedRecords.add(record);
                    e.printStackTrace();
                    LogInFile.write("/sdcard/debug.txt", e.getMessage());
                }
            }
        });
    }

    private void recordData(final List<Record> records) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MySqlUtil.insertData(records);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case TASK_START:
                    customers = (List<Customer>) msg.obj;
                    if (!customers.isEmpty()) {
                        for (Customer customer : customers) {
                            customer.setRobotID(sn);
                        }
                        runTask(customers.get(0));
                        customers.remove(0);
                    }
                    break;

                case TASK_END:
                    ssm.stopSpeakingLoop();

                    //派送失败处理
                    if (msg.obj != null) {
                        Customer customer = (Customer) msg.obj;
                        failedCustomers.add(customer);
                        SmsUtil.asyncSend(customer.getPhoneNum(), SmsUtil.EX_FAIL, null);
                    }

                    //接着送下一个快递或返回
                    if (!customers.isEmpty()) {
                        runTask(customers.get(0));
                        customers.remove(0);
                    } else {
                        back();
                    }
                    break;

                //返回主界面
                case GO_HOME:
                    failedCustomers.clear();
                    FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
                    ft.replace(R.id.container, new MTStartFragment());
                    ft.commit();
                    break;

                case INIT:
                    Record record = new Record(sn, "注册", area);
                    recordData(record);

                    initPoseTalker.sendMsg(initPosition[0], initPosition[1], initPosition[2], initPosition[3]);
//                    liftInitPoseTalker.sendMsg(initFloor, initPosition[0], initPosition[1], initPosition[2], initPosition[3]);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                           goExpressPosition();
                        }
                    }, 1000);
                    break;

                default:
            }
        }
    };

}
