package com.ut.lulyfan.exrobot.util.liftUtil;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * Created by Administrator on 2017/12/18/018.
 */

public class LiftUtil {

    private static final String TAG = "lift";

    private static final int CMD_CALL_INBOUND = 30;         //机器人内呼指令
    private static final int CMD_CALL_OUTBOUND = 31;         //机器人外呼指令（上下召）
    private static final int CMD_QUERY = 32;         //机器人查询指令
    private static final int CMD_KEEP_DOOR_OPEN = 33;         //机器人开门保持指令
    private static final int CMD_REALEASE_DOOR = 34;         //机器人释放开门保持指令
    private static final int CMD_HEART = 35;         //机器人心跳包指令
    private static final int CMD_RECEIVE_LIFT_STATE = 40;  //收到电梯下发的状态

    boolean isReceiveCMD_CALL_INBOUND;
    boolean isReceiveCMD_CALL_OUTBOUND;
    boolean isReceiveCMD_KEEP_DOOR_OPEN;
    boolean isReceiveCMD_REALEASE_DOOR;
    boolean isReceiveCMD_QUERY;

    //指令上下召
    public static final int UP = 0x01;   //上召
    public static final int DOWN = 0x02;  //下召

    //电梯主动发送的状态指令/查询指令
    public static final int STOP = 0x04;   //运行方向停止
    public static final int DOOR_OPENED = 0x01;  //开门到位

    private static final int FINDING_HEAD = 0;
    private static final int FINDING_TAIL = 1;

    private static final int HEAD = 0x02;
    private static final int TAIL = 0x03;

    private int robotADDR = 0x80;
    private int liftADDR = 0x01;
    private int PANID_HIGH = 0x00;  //无线模块网络识别码高字节
    private int PANID_LOW = 0x01;   //无线模块网络识别码低字节
    private int state;       //解析状态

    private ByteBuffer sendBuffer = ByteBuffer.allocate(100);
    private ByteBuffer receiveBuffer = ByteBuffer.allocate(100);  //一条消息缓冲区，不装头和尾
    private ByteBuffer restoreBuffer = ByteBuffer.allocate(100);
    private ByteBuffer transformBuffer = ByteBuffer.allocate(100);

    private Executor executor;

    public LiftUtil(LiftService liftService) {
        executor = liftService.executor;
    }

    public void parse(byte[] bytes) {

        for (byte b : bytes) {
            switch (state) {
                case FINDING_HEAD:
                    if (b == HEAD) {
                        receiveBuffer.clear();
                        state = FINDING_TAIL;
                    }
                    break;
                case FINDING_TAIL:
                    if (b == HEAD) {
                        receiveBuffer.clear();
                    } else if (b == TAIL) {
                        handleMsg(receiveBuffer);
                        state = FINDING_HEAD;
                    } else {
                        if (!receiveBuffer.hasRemaining()) {   //缓冲区满了，还没找到消息尾，重置缓冲区
                            receiveBuffer.clear();
                            return;
                        }
                        receiveBuffer.put(b);
                    }
                    break;
            }
        }
    }

    private void handleMsg(ByteBuffer byteBuffer) {

        System.out.println("handleMsg");

        final ByteBuffer buffer = restore(byteBuffer);
        int length = buffer.limit();
        if (length < 6)
            return;

        //校验和
        int sumCheck = byteToUnsignedInt(buffer.get(length-2)) * 256 + byteToUnsignedInt(buffer.get(length-1));
        int sum = 0;
        for (int i=0; i<length-2; i++) {
            sum += byteToUnsignedInt(buffer.get());
        }

        if (sum != sumCheck) {
            return;
        }

        //检查目标地址
        int dstADDR = byteToUnsignedInt(buffer.get(4));
        if (dstADDR != robotADDR && dstADDR != 0xff) {
            return;
        }

        int cmd = byteToUnsignedInt(buffer.get(0));
        final byte[] array = new byte[buffer.limit()];
        System.arraycopy(buffer.array(), 0, array, 0, array.length);

        switch (cmd) {
            case CMD_CALL_INBOUND:
                isReceiveCMD_CALL_INBOUND = true;
                if (replyListener != null) {
                    replyListener.replyCallInbound(array);
                }
                break;

            case CMD_CALL_OUTBOUND:
                isReceiveCMD_CALL_OUTBOUND = true;
                break;

            case CMD_QUERY:
                isReceiveCMD_QUERY = true;
                if (replyListener != null) {
                    replyListener.replyQuery(array);
                }
                break;

            case CMD_KEEP_DOOR_OPEN:
                isReceiveCMD_KEEP_DOOR_OPEN = true;
                if (replyListener != null) {
                    replyListener.replyOpen(array);
                }
                break;

            case CMD_REALEASE_DOOR:
                isReceiveCMD_REALEASE_DOOR = true;
                if (replyListener != null) {
                    replyListener.replyRealease(array);
                }
                break;

            case CMD_HEART:

                break;

            case CMD_RECEIVE_LIFT_STATE:

                if (msgListener != null) {
                    msgListener.receiveLiftState(array);
                }
                break;

            default:
        }

    }

    //处理转义
    private ByteBuffer restore(ByteBuffer buffer) {
        ByteBuffer dstBuffer = restoreBuffer;
        dstBuffer.clear();

        buffer.flip();

        boolean flag = false;
        while (buffer.hasRemaining()) {
            int data = buffer.get();
            switch (data) {
                case 0x04:
                    if (flag) {
                        dstBuffer.put((byte) 0x04);   //0x04 0x04 to 0x04
                        flag = false;
                    } else
                        flag = true;
                    break;

                case 0x06:
                    if (flag) {
                        dstBuffer.put((byte) 0x02);   //0x04 0x06 to 0x02
                        flag = false;
                    } else
                        dstBuffer.put((byte) data);
                    break;

                case 0x07:
                    if (flag) {
                        dstBuffer.put((byte) 0x03);    //0x04 0x07 to 0x03
                        flag = false;
                    } else
                        dstBuffer.put((byte) data);
                    break;

                default:
                    dstBuffer.put((byte) data);
            }
        }
        dstBuffer.flip();

        return dstBuffer;
    }

    private ByteBuffer transform(ByteBuffer buffer) {
        ByteBuffer dstBuffer = transformBuffer;
        dstBuffer.clear();

        buffer.flip();
        int limit = buffer.limit();

        while (buffer.hasRemaining()) {

            int data = buffer.get();
            if (buffer.position() == 1 || buffer.position() == limit) {
                dstBuffer.put((byte) data);
                continue;
            }

            switch (data) {
                case 0x02:
                    dstBuffer.put((byte) 0x04);
                    dstBuffer.put((byte) 0x06);
                    break;

                case 0x03:
                    dstBuffer.put((byte) 0x04);
                    dstBuffer.put((byte) 0x07);
                    break;

                case 0x04:
                    dstBuffer.put((byte) 0x04);
                    dstBuffer.put((byte) 0x04);
                    break;

                default:
                    dstBuffer.put((byte) data);
            }
        }
        dstBuffer.flip();

        return dstBuffer;
    }

    private MsgListener msgListener;

    public void setMsgListener(MsgListener msgListener) {
        this.msgListener = msgListener;
    }

    public interface MsgListener {
        void receiveLiftState(byte[] data);
    }

    private ReplyListener replyListener;

    public void setReplyListener(ReplyListener replyListener) {
        this.replyListener = replyListener;
    }

    interface ReplyListener {
        void replyCallInbound(byte[] data);
        void replyOpen(byte[] data);
        void replyRealease(byte[] data);
        void replyQuery(byte[] data);
    }

    private SendListener sendListener;

    public void setSendListener(SendListener sendListener) {
        this.sendListener = sendListener;
    }

    public interface SendListener {
        void send(ByteBuffer byteBuffer);
    }

    public ByteBuffer callInbound(int dstFloor) {
        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_CALL_INBOUND);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);
        sendBuffer.put((byte) dstFloor);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    public ByteBuffer callOutbound(int curFloor, int upOrDown) {

        if (upOrDown != UP && upOrDown != DOWN) {
            Log.e(TAG, "上下召指令方向错误");
            return null;
        }

        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_CALL_OUTBOUND);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);
        sendBuffer.put((byte) curFloor);
        sendBuffer.put((byte) upOrDown);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    public ByteBuffer queryState() {
        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_QUERY);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    public ByteBuffer keepDoorOpen() {
        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_KEEP_DOOR_OPEN);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    public ByteBuffer realeaseDoor() {
        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_REALEASE_DOOR);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    public ByteBuffer sendHeart() {
        sendBuffer.clear();

        sendBuffer.put((byte) HEAD);
        sendBuffer.put((byte) CMD_HEART);
        sendBuffer.put((byte) PANID_HIGH);
        sendBuffer.put((byte) PANID_LOW);
        sendBuffer.put((byte) robotADDR);
        sendBuffer.put((byte) liftADDR);

        putCheckSum(sendBuffer);
        sendBuffer.put((byte) TAIL);

        ByteBuffer byteBuffer = transform(sendBuffer);
        if (sendListener != null) {
            sendListener.send(byteBuffer);
        }
        return byteBuffer;
    }

    private void putCheckSum(ByteBuffer byteBuffer) {
        int sum = 0;
        for (int i=1; i<byteBuffer.position(); i++) {
            int num = byteToUnsignedInt(sendBuffer.get(i));
            sum += num;
        }

        int sum_high = sum / 256;
        int sum_low = sum % 256;

        byteBuffer.put((byte) sum_high);
        byteBuffer.put((byte) sum_low);
    }

    public void setLiftADDR(int liftADDR) {
        this.liftADDR = liftADDR;
    }

    public int getLiftADDR() {
        return liftADDR;
    }

    public static String byteToHexUnsignedString(byte b) {

        int num = byteToUnsignedInt(b);

        String str = Integer.toHexString(num);

        if (str.length() == 1)
            str = "0" + str;

        return str.toUpperCase();
    }

    public static int byteToUnsignedInt(byte b) {

//        int num = 0;
//        for (int i=0; i<8; i++) {
//            num += ((b >>> i) & 0x1) * Math.pow(2, i);
//        }

        int num = b & 0x7f;
        if (b < 0)
            num += 128;

        return num;
    }
}
