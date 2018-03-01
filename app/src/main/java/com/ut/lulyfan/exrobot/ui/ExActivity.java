package com.ut.lulyfan.exrobot.ui;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.iflytek.cloud.SpeechError;
import com.ut.lulyfan.exrobot.R;
import com.ut.lulyfan.exrobot.debug.LogInFile;
import com.ut.lulyfan.exrobot.model.Customer;
import com.ut.lulyfan.exrobot.model.Record;
import com.ut.lulyfan.exrobot.ros.ClientActivity;
import com.ut.lulyfan.exrobot.ros.NewGoTalker;
import com.ut.lulyfan.exrobot.ui.meituan.MTArriveFragment;
import com.ut.lulyfan.exrobot.ui.meituan.MTStartFragment;
import com.ut.lulyfan.exrobot.util.ExcelUtil;
import com.ut.lulyfan.exrobot.util.MySqlUtil;
import com.ut.lulyfan.exrobot.util.SmsUtil;
import com.ut.lulyfan.exrobot.util.liftUtil.DebugView;
import com.ut.lulyfan.exrobot.util.liftUtil.LiftControl;
import com.ut.lulyfan.exrobot.util.liftUtil.LiftPoint;
import com.ut.lulyfan.exrobot.util.liftUtil.LiftService;
import com.ut.lulyfan.voicelib.voiceManager.SpeechSynthesizeManager;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
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

    private List<Customer> allCustomers = new ArrayList<>();  //快递的送达客户
    private List<Customer> dstCustomers = new ArrayList<>();  //当前要送达的某个楼层的客户
    private ArrayList<Customer> failedCustomers = new ArrayList<>();  //快递未领取的客户
    private int initFloor ;  //机器人的初始化楼层
    private int curFloor ;      //机器人当前所处楼层
    private int dstFloor;      //机器人要去的目标楼层
    private double[] initPosition = new double[4];
    private double[] exPosition = new double[4];    //获取快递的坐标点
    private long lastTime;   //检测长按的时间
    private long blockSynEndTime;     //阻塞时语音合成结束时间
    private InitFragment initFragment;
    private MoveFragment moveFragment;
    private BatteryView batteryView;
    public SpeechSynthesizeManager ssm;
    static Executor executor = Executors.newCachedThreadPool();
    public static String sn;
    public static String area;
    private List<Record> failedRecords = new ArrayList<>();
    private List<LiftPoint> liftPoints = new ArrayList<>();
    private LiftService liftService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ex);

        DebugView.getInstance(this);

        batteryView = (BatteryView) findViewById(R.id.battery);

        ssm = new SpeechSynthesizeManager(this, false, handler);
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

        getLiftPoints("/sdcard/UTRobot/liftPoint.xls");

        Intent intent = new Intent(this, LiftService.class);
        bindService(intent, liftServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection liftServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            liftService = ((LiftService.LocalBinder) service).getService();
            liftService.init(ExActivity.this, new LiftService.InitListener() {
                @Override
                public void handleInit(boolean result) {
                    if (!result) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ExActivity.this);
                        builder.setTitle("提示")
                                .setMessage("串口初始化失败!")
                                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ExActivity.this.finish();
                                    }
                                });
                        builder.show();
                    }
                }
            });
            liftService.setLiftPoints(liftPoints);
            System.out.println("connect liftService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    float firstX;
    float firstY;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.i("ui", "activity onTouchEvent");
        long gapTime;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTime = System.currentTimeMillis();
                firstX = ev.getRawX();
                firstY = ev.getRawY();
            case MotionEvent.ACTION_UP:
                gapTime = System.currentTimeMillis() - lastTime;
                if (gapTime >= 5000) {
                    Intent intent = new Intent(this, SettingActivity.class);
                    startActivity(intent);
                }

                int moveX = (int) (ev.getRawX() - firstX);
                if (moveX > 1000) {
                    DebugView.getInstance(this).show();
                }

                int moveY = (int) (ev.getRawY() - firstY);
                if (moveY > 700) {
                    if (liftService != null) {
                        liftService.realeaseDoor();
                    }
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
        initFloor = curFloor = Integer.parseInt(sfloor);

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

    private void getLiftPoints(String path) {
        try {
            liftPoints = ExcelUtil.getLiftPoints(path);
        } catch (InvalidFormatException e) {
            Toast.makeText(this, "InvalidFormatException:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "数据异常:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "IOException:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        DebugView.destory();
        nodeMainExecutorService.forceShutdown();
        ssm.destory();
        unbindService(liftServiceConnection);
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

    //本楼层快递任务
    public void sameFloorTask(final Customer customer) {

//        ssm.startSpeaking("开始派送"+customer.getName()+"的外卖");

        newGoTalker.sendMsg(NewGoTalker.NORMAL_MODE, customer.getX(), customer.getY(), customer.getZ(), customer.getW());
        //切换到移动的fragment
        moveFragment = (MoveFragment) MoveFragment.newInstance("正在派送"+customer.getName()+"的外卖");
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container,  moveFragment);
        ft.commit();

       setCode(customer);

        setArriveHandler(new ArriveHandler() {
            @Override
            public void hanldArrive() {
                handleExpressArrive(customer);
            }
        });

        dstCustomers.remove(0);
    }

    private void setCode(Customer customer) {
        String nowTime = System.currentTimeMillis() + "";
        String code = nowTime.substring(nowTime.length() - 4);
        customer.setCode(code);
        SmsUtil.asyncSend(customer.getPhoneNum(), SmsUtil.MT_START, "{\"code\":\" "+ code + "\"}");
    }

    private void handleExpressArrive(Customer customer) {
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

    //去取快递的地点（领取快递）
    public void goExpressPosition() {

        newGoTalker.sendMsg(NewGoTalker.NORMAL_MODE, exPosition[0], exPosition[1], exPosition[2], exPosition[3]);

        moveFragment = (MoveFragment) MoveFragment.newInstance("正在前往前台");
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

    private void takeLift(int curFloor, int dstFloor, LiftControl.LiftListener liftListener) {

        moveFragment = (MoveFragment) MoveFragment.newInstance("正在前往电梯口");
        liftService.setStateListener(moveFragment);

        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container,  moveFragment);
        ft.commit();

        liftService.takeLift(curFloor, dstFloor, liftListener);
    }

    private void back() {
        if (curFloor != initFloor) {
            liftService.setFirstTaskPoint(exPosition);
            takeLift(curFloor, initFloor, new LiftControl.LiftListener() {
                @Override
                public void finish() {
                    curFloor = initFloor;
                    handleBackArrive();
                }

                @Override
                public void goOutFinish() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (moveFragment != null) {
                                moveFragment.setGoOutFinishTip("外卖派送完毕,正在返回");
                            }
                        }
                    });
                }
            });
        } else {
            sameFloorBack();
        }
    }

    private void sameFloorBack() {

        newGoTalker.sendMsg(NewGoTalker.NORMAL_MODE, exPosition[0], exPosition[1], exPosition[2], exPosition[3]);

//        ssm.startSpeaking("快递派送完毕,开始返回...");
        moveFragment = (MoveFragment) MoveFragment.newInstance("外卖派送完毕,正在返回");
        FragmentTransaction ft = ExActivity.this.getFragmentManager().beginTransaction();
        ft.replace(R.id.container, moveFragment);
        ft.commit();

        setArriveHandler(new ArriveHandler() {
            @Override
            public void hanldArrive() {
               handleBackArrive();
            }
        });
    }

    private void handleBackArrive() {
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

    private void startTasks() {
        if (allCustomers == null)
            return;

        dstFloor = allCustomers.get(0).getFloor();
        for (Customer customer : allCustomers) {
            if (customer.getFloor() == dstFloor) {
                dstCustomers.add(customer);
                allCustomers.remove(customer);
            }
        }

        for (Customer customer : dstCustomers) {
            customer.setRobotID(sn);
        }

        if (curFloor == dstFloor) {
            sameFloorTask(dstCustomers.get(0));
        } else {

            final Customer customer = dstCustomers.get(0);
            dstCustomers.remove(0);

            double[] outPoint = new double[4];
            outPoint[0] = customer.getX();
            outPoint[1] = customer.getY();
            outPoint[2] = customer.getZ();
            outPoint[3] = customer.getW();
            liftService.setFirstTaskPoint(outPoint);

            takeLift(curFloor, dstFloor, new LiftControl.LiftListener() {
                @Override
                public void finish() {
                    curFloor = dstFloor;
                    handleExpressArrive(customer);
                }

                @Override
                public void goOutFinish() {
                    setCode(customer);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (moveFragment != null) {
                                moveFragment.setGoOutFinishTip("正在派送"+customer.getName()+"的外卖");
                            }
                        }
                    });
                }
            });
        }
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case TASK_START:
                    allCustomers = (List<Customer>) msg.obj;
                    startTasks();
                    break;

                case TASK_END:
                    ssm.stopSpeakingLoop();

                    //派送失败处理
                    if (msg.obj != null) {
                        Customer customer = (Customer) msg.obj;
                        failedCustomers.add(customer);
                        SmsUtil.asyncSend(customer.getPhoneNum(), SmsUtil.EX_FAIL, null);
                    }

                    //接着送下一个快递或送其他楼层任务或返回
                    if (!dstCustomers.isEmpty()) {
                        sameFloorTask(dstCustomers.get(0));
                    } else if (!allCustomers.isEmpty()) {
                        startTasks();
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

                    liftInitPoseTalker.sendMsg(100 + initFloor, initPosition[0], initPosition[1], initPosition[2], initPosition[3]);
                    setLiftInitHandler(new LiftInitHandler() {
                        @Override
                        public void handleLiftInit(int result) {
                            if (result != -1) {
                                DebugView.println("change map success");
                                goExpressPosition();
                            }
                        }
                    });

                    break;

                default:
            }
        }
    };

}
