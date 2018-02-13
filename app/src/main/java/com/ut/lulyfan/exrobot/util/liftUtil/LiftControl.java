package com.ut.lulyfan.exrobot.util.liftUtil;


import com.ut.lulyfan.exrobot.ros.ClientActivity;
import com.ut.lulyfan.exrobot.ros.NewGoTalker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/1/9/009.
 */

public class LiftControl implements LiftUtil.MsgListener, LiftUtil.ReplyListener{
    LiftUtil liftUtil;
    ClientActivity clientActivity;
    ExecutorService executor;
    ScheduledExecutorService scheduledExecutor;

    private int curFloor;
    private int dstFloor;

    private int state;
    private static final int IDLE = 0;
    private static final int WAITING_LIFT_ARRIVE_DST_FLOOR = 1; //等待电梯到达目标楼层
    private static final int WAITING_LIFT_ARRIVE_CUR_FLOOR = 2; //等待电梯到达当前楼层
    private static final int ENTERING = 3;   //正在进行入梯操作
    private static final int ENTER_FINSH = 4;    //入梯完成
    private static final int OUTING = 5;     //正在进行出梯操作
    private static final int OUT_FINSH = 6;      //出梯完成

    private boolean isArriveCur;
    private boolean isArriveDst;
    private boolean isInLift;       //是否在电梯内
    private boolean isOutLift;      //是否在电梯外
    private int enterTimeout = 20 * 1000;
    private int outTimeout = 20 * 1000;

    private double[] curFloorOutLiftPoint = new double[4];
    private double[] curFloorInLiftPoint = new double[4];
    private double[] dstFloorOutLiftPoint = new double[4];
    private double[] dstFloorInLiftPoint = new double[4];
    private double[] outPoint;  //出梯后去的第一个点
    private boolean stopOpen;

    private boolean isRealeasing;   //防止多次释放
    private boolean enterFailedFlag;   //防止多次调用进梯失败
    private boolean outFailedFlag;     //防止多次调用出梯失败

    public LiftControl(ClientActivity activity, LiftUtil liftUtil, LiftService liftService) {
        this.clientActivity = activity;
        this.liftUtil = liftUtil;
        executor = liftService.executor;
        scheduledExecutor = liftService.scheduledExecutor;
    }

    public void takeLift(final int curFloor, final int dstFloor) {

        state = IDLE;

        if (curFloor == dstFloor)
            return;

        clientActivity.setLiftPositionListener(new ClientActivity.LiftPositionListener() {
            @Override
            public void start() {
                DebugView.println("start go in slowPath");
            }

            @Override
            public void end() {
                DebugView.println("end slowPath");

                if (state == ENTERING) {
                    isInLift = true;
                } else if (state == OUTING) {
                    isOutLift = true;
                    if (stateListener != null) {
                        stateListener.goTaskPoint();
                    }
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendRealeaseDoor();
                    }
                });
            }
        });

        DebugView.println("takeLift curFloor:" + curFloor + " dstFloor:" + dstFloor);
        this.curFloor = curFloor;
        this.dstFloor = dstFloor;
        clientActivity.newGoTalker.sendMsg(NewGoTalker.NORMAL_MODE, curFloorOutLiftPoint[0], curFloorOutLiftPoint[1], curFloorOutLiftPoint[2], curFloorOutLiftPoint[3]);
        DebugView.println("go curFloorOutLiftPoint:" + curFloorOutLiftPoint[0] + " " + curFloorOutLiftPoint[1] +" " +curFloorOutLiftPoint[2] + " " + curFloorOutLiftPoint[3]);
        clientActivity.setArriveHandler(new ClientActivity.ArriveHandler() {
            @Override
            public void hanldArrive() {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        DebugView.println("arrive curFloorOutLiftPoint");
                        callLiftToCur(curFloor);
                    }
                });
                clientActivity.setArriveHandler(null);
            }
        });
    }

    public void liftToCurFloor() {
        DebugView.println("lift arrive curFloor");
        isArriveCur = true;
        isInLift = false;
        enterFailedFlag = false;
        state = ENTERING;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sendKeepDoorOpen(5, 500);
            }
        });

        if (stateListener != null) {
            stateListener.goIn();
        }

        DebugView.println("go curFloorInLiftPoint:" + curFloorInLiftPoint[0] + " " + curFloorInLiftPoint[1] + " " + curFloorInLiftPoint[2] + " " + curFloorInLiftPoint[3]);
        clientActivity.newGoTalker.sendMsg(NewGoTalker.LIFT_GOIN_MODE, curFloorInLiftPoint[0], curFloorInLiftPoint[1], curFloorInLiftPoint[2], curFloorInLiftPoint[3]);
        clientActivity.setArriveHandler(new ClientActivity.ArriveHandler() {
            @Override
            public void hanldArrive() {
                isInLift = true;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        state = ENTER_FINSH;
                        DebugView.println("arrive curFloorInLiftPoint");
                        sendRealeaseDoor();

                        clientActivity.liftInitPoseTalker.sendMsg(dstFloor + 100, dstFloorInLiftPoint[0], dstFloorInLiftPoint[1], dstFloorInLiftPoint[2], dstFloorInLiftPoint[3]);
                        DebugView.println("start change map " + (dstFloor + 100));
                        clientActivity.setLiftInitHandler(new ClientActivity.LiftInitHandler() {
                            @Override
                            public void handleLiftInit(int result) {
                                if (result != -1) {
                                    DebugView.println("change map success");
                                }
                            }
                        });

                        callLiftToDst(dstFloor);
                    }
                });
                clientActivity.setArriveHandler(null);
            }
        });

        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                if (state == ENTERING && !isInLift) {
                    DebugView.println("enter lift timeout");
                    enterFailed();
                }
            }
        }, enterTimeout, TimeUnit.MILLISECONDS);

        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                sendQuery();
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }

    private void enterFailed() {

        if (enterFailedFlag) {
            return;
        }
        enterFailedFlag = true;

        DebugView.println("enter Failed");
        DebugView.println("back curFloorOutLiftPoint:" + curFloorOutLiftPoint[0] + " " + curFloorOutLiftPoint[1] +" " +curFloorOutLiftPoint[2] + " " + curFloorOutLiftPoint[3]);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sendRealeaseDoor();
            }
        });

        if (stateListener != null) {
            stateListener.goInFailed();
        }

        clientActivity.newGoTalker.sendMsg(NewGoTalker.LIFT_GOIN_MODE, curFloorOutLiftPoint[0], curFloorOutLiftPoint[1], curFloorOutLiftPoint[2], curFloorOutLiftPoint[3]);
        clientActivity.setArriveHandler(new ClientActivity.ArriveHandler() {
            @Override
            public void hanldArrive() {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        DebugView.println("arrive curFloorOutLiftPoint");
                        callLiftToCur(curFloor);
                    }
                });
                clientActivity.setArriveHandler(null);
            }
        });
    }

    public void liftToDstFloor() {

        DebugView.println("lift arrive dstFloor");
        isArriveDst = true;
        isOutLift = false;
        outFailedFlag = false;
        state = OUTING;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sendKeepDoorOpen(5, 500);
            }
        });

        if (stateListener != null) {
            stateListener.goOut();
        }


        if (outPoint == null) {
            DebugView.println("go dstFloorOutLiftPoint:" + dstFloorOutLiftPoint[0] + " " + dstFloorOutLiftPoint[1] + " " + dstFloorOutLiftPoint[2] + " " + dstFloorOutLiftPoint[3]);
            clientActivity.newGoTalker.sendMsg(NewGoTalker.LIFT_GOOUT_MODE, dstFloorOutLiftPoint[0], dstFloorOutLiftPoint[1], dstFloorOutLiftPoint[2], dstFloorOutLiftPoint[3]);
        } else {
            DebugView.println("go outPoint:" + outPoint[0] + " " + outPoint[1] + " " + outPoint[2] + " " + outPoint[3]);
            clientActivity.newGoTalker.sendMsg(NewGoTalker.LIFT_GOOUT_MODE, outPoint[0], outPoint[1], outPoint[2], outPoint[3]);
        }

        clientActivity.setArriveHandler(new ClientActivity.ArriveHandler() {
            @Override
            public void hanldArrive() {
                isOutLift = true;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        state = IDLE;
                        DebugView.println("takeLift end");
                        outPoint = null;
                        sendRealeaseDoor();

                        clientActivity.setLiftPositionListener(null);
                        if (liftListener != null) {
                            liftListener.finish();
                        }
                    }
                });
                clientActivity.setArriveHandler(null);
            }
        });

        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                if (state == OUTING && !isOutLift) {
                    DebugView.println("out lift timeout");
                    outFailed();
                }
            }
        }, outTimeout, TimeUnit.MILLISECONDS);

        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                sendQuery();
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }

    private void outFailed() {

        if (outFailedFlag) {
            return;
        }
        outFailedFlag = true;

        DebugView.println("out failed");
        DebugView.println("back dstFloorInLiftPoint");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                sendRealeaseDoor();
            }
        });

        if (stateListener != null) {
            stateListener.goOutFailed();
        }

        clientActivity.newGoTalker.sendMsg(NewGoTalker.LIFT_GOOUT_MODE, dstFloorInLiftPoint[0], dstFloorInLiftPoint[1], dstFloorInLiftPoint[2], dstFloorInLiftPoint[3]);
        clientActivity.setArriveHandler(new ClientActivity.ArriveHandler() {
            @Override
            public void hanldArrive() {
                DebugView.println("arrive dstFloorInLiftPoint");
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        callLiftToDst(dstFloor);
                    }
                });
            }
        });
    }

    public void setLiftPoints(double[] curFloorInLiftPoint, double[] curFloorOutLiftPoint, double[] dstFloorInLiftPoint, double[] dstFloorOutLiftPoint) {
        this.curFloorInLiftPoint = curFloorInLiftPoint;
        this.curFloorOutLiftPoint = curFloorOutLiftPoint;
        this.dstFloorInLiftPoint = dstFloorInLiftPoint;
        this.dstFloorOutLiftPoint = dstFloorOutLiftPoint;
    }

    //设置出梯去的第一个点
    public void setOutPoint(double[] point) {
        outPoint = point;
    }

    private void sendQuery() {
        do {
            DebugView.println("query lift state...");
            liftUtil.queryState();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while ((state == ENTERING && !isInLift) || (state == OUTING && !isOutLift));
    }

    private void sendKeepDoorOpen(int count, long gapTime) {
        liftUtil.isReceiveCMD_KEEP_DOOR_OPEN = false;
        stopOpen = false;
        do {
            DebugView.println("keepDoorOpen");
            liftUtil.keepDoorOpen();
            count --;
            try {
                Thread.sleep(gapTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while (!liftUtil.isReceiveCMD_KEEP_DOOR_OPEN && !stopOpen && count > 0);
    }

    public void sendRealeaseDoor() {

        if (isRealeasing) {
            return;
        }
        isRealeasing = true;

        DebugView.println("start realeaseDoor");
        liftUtil.isReceiveCMD_REALEASE_DOOR = false;
        stopOpen = true;
        do {
            DebugView.println("realeaseDoor");
            liftUtil.realeaseDoor();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (!liftUtil.isReceiveCMD_REALEASE_DOOR);

        isRealeasing = false;
    }

    private void sendCallInbound(int dstFloor) {
        do {
            liftUtil.callInbound(dstFloor);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while (!liftUtil.isReceiveCMD_CALL_INBOUND);
    }

    private void sendCallOutbound(int curLift, int dstLift) {
        liftUtil.isReceiveCMD_CALL_OUTBOUND = false;
        do {
            int orientation = curLift > dstLift ? LiftUtil.UP : LiftUtil.DOWN;
            liftUtil.callOutbound(curLift, orientation);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while (!liftUtil.isReceiveCMD_CALL_OUTBOUND);
    }

    private void callLiftToCur(int curFloor) {
        DebugView.println("callLiftToCur");
        if (stateListener != null) {
            stateListener.callCurFloor(curFloor);
        }

        state = WAITING_LIFT_ARRIVE_CUR_FLOOR;
        isArriveCur = false;
        do {
            liftUtil.callInbound(curFloor);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while (!isArriveCur);
    }

    private void callLiftToDst(int dstFloor) {
        DebugView.println("callLiftToDst");
        if (stateListener != null) {
            stateListener.callDstFloor(dstFloor);
        }

        state = WAITING_LIFT_ARRIVE_DST_FLOOR;
        isArriveDst = false;
        do {
            liftUtil.callInbound(dstFloor);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while (!isArriveDst);
    }

    @Override
    public void receiveLiftState(byte[] data) {

        int floor = LiftUtil.byteToUnsignedInt(data[6]);
        int liftAddr = LiftUtil.byteToUnsignedInt(data[3]);

//        DebugView.println("receive lift state floor:" + floor);

        //过滤无效楼层
        if (floor == 0x00 || floor == 0xFF) {
            return;
        }

        if (state == ENTERING && !isInLift && floor != curFloor) {
            enterFailed();
            return;
        }

        if (state == OUTING && !isOutLift && floor != dstFloor) {
            outFailed();
            return;
        }

//        if (data[8] != LiftUtil.DOOR_OPENED && data[5] != LiftUtil.STOP) {
//           return;
//        }

        if (state != WAITING_LIFT_ARRIVE_CUR_FLOOR && state != WAITING_LIFT_ARRIVE_DST_FLOOR) {
            return;
        }

        if (data[5] != LiftUtil.STOP) {
            return;
        }

        if (floor == curFloor && liftAddr == liftUtil.getLiftADDR() && state == WAITING_LIFT_ARRIVE_CUR_FLOOR) {
//            liftUtil.setLiftADDR(data[4]);
            liftToCurFloor();
        }
        else if (floor == dstFloor && liftAddr == liftUtil.getLiftADDR() && state == WAITING_LIFT_ARRIVE_DST_FLOOR) {
            liftToDstFloor();
        }

    }

    public int getEnterTimeout() {
        return enterTimeout;
    }

    public void setEnterTimeout(int enterTimeout) {
        this.enterTimeout = enterTimeout;
    }

    public int getOutTimeout() {
        return outTimeout;
    }

    public void setOutTimeout(int outTimeout) {
        this.outTimeout = outTimeout;
    }

    private LiftListener liftListener;

    public void setLiftListener(LiftListener liftListener) {
        this.liftListener = liftListener;
    }

    @Override
    public void replyCallInbound(byte[] data) {

    }

    @Override
    public void replyOpen(byte[] data) {

    }

    @Override
    public void replyRealease(byte[] data) {

    }

    @Override
    public void replyQuery(byte[] data) {

        int floor = LiftUtil.byteToUnsignedInt(data[6]);

        if (floor == 0x00 || floor == 0xFF) {
            return;
        }

        DebugView.println("receive query reply floor:" + floor);

        if (state == ENTERING && !isInLift && floor != curFloor) {
            enterFailed();
            return;
        }

        if (state == OUTING && !isOutLift && floor != dstFloor) {
            outFailed();
            return;
        }
    }

    public interface LiftListener {
        void finish();
    }

    private StateListener stateListener;

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public interface StateListener {
        void callCurFloor(int curFloor);
        void goIn();
        void goOut();
        void goInFailed();
        void goOutFailed();
        void callDstFloor(int dstFloor);
        void goTaskPoint();   //出梯完成后去第一个任务点时调用
    }
}
