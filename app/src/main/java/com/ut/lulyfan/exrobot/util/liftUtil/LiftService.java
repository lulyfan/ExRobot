package com.ut.lulyfan.exrobot.util.liftUtil;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.ut.lulyfan.exrobot.ros.ClientActivity;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LiftService extends Service implements LiftUtil.SendListener{
    private static final String TAG = "LiftService";
    private final IBinder mBinder = new LocalBinder();

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private static final int BAUD_RATE = 9600;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private Long lastDataTime = 0L;
    private ByteBuffer dataBuffer = ByteBuffer.allocate(1000);

    private LiftUtil liftUtil;
    private LiftControl liftControl;
    private List<LiftPoint> liftPoints;

    ExecutorService executor = Executors.newCachedThreadPool();;
    ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public LiftService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        device = findSerialPortDevice();

        return mBinder;
    }

    public void init(ClientActivity activity, InitListener initListener) {

        setInitListener(initListener);

        liftUtil = new LiftUtil(this);
        liftControl = new LiftControl(activity, liftUtil, this);
        liftUtil.setMsgListener(liftControl);
        liftUtil.setReplyListener(liftControl);

        if (device != null)
            requestUserPermission();
    }

    public void realeaseDoor() {
        if (liftControl != null) {
            liftControl.sendRealeaseDoor();
        }
    }

    //设置出梯后去的第一个任务点
    public void setFirstTaskPoint(double[] outPoint) {
        if (liftControl != null) {
            liftControl.setFirstTaskPoint(outPoint);
        }
    }

    public void setLiftPoints(List<LiftPoint> liftPoints) {
        this.liftPoints = liftPoints;
    }

    public void setStateListener(LiftControl.StateListener stateListener) {
        if (liftControl != null) {
            liftControl.setStateListener(stateListener);
        }
    }

    public void takeLift(int curFloor, int dstFloor, LiftControl.LiftListener liftListener) {
        if (liftControl == null) {
            Log.e(TAG, "liftControl == null, please init first!");
            return;
        }

        if (liftPoints == null) {
            Log.e(TAG, "liftPoints == null, please set liftPoints first");
            return;
        }

        double[] curFloorOutLiftPoint = new double[4];
        double[] curFloorInLiftPoint = new double[4];
        double[] dstFloorOutLiftPoint = new double[4];
        double[] dstFloorInLiftPoint = new double[4];

        for (LiftPoint point : liftPoints) {
            if (point.getFloor() == curFloor ) {
                curFloorInLiftPoint = point.inPoint;
                curFloorOutLiftPoint = point.outPoint;
            } else if(point.getFloor() == dstFloor)  {
                dstFloorInLiftPoint = point.inPoint;
                dstFloorOutLiftPoint = point.outPoint;
            }
        }
        liftControl.setLiftPoints(curFloorInLiftPoint, curFloorOutLiftPoint, dstFloorInLiftPoint, dstFloorOutLiftPoint);
        liftControl.setLiftListener(liftListener);
        liftControl.takeLift(curFloor, dstFloor);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(usbReceiver);
        if (serialPort != null) {
            serialPort.syncClose();
        }
        executor.shutdown();
        scheduledExecutor.shutdown();
        return super.onUnbind(intent);
    }

    @Override
    public void send(ByteBuffer byteBuffer) {
        byte[] data = change(byteBuffer);
        if (serialPort != null)
            serialPort.write(data);
    }

    private byte[] change(ByteBuffer byteBuffer) {

        int limit = byteBuffer.limit();
        byte[] data = new byte[limit];

        for (int i=0; i<limit; i++){
            data[i] = byteBuffer.get(i);
        }

        return data;
    }

    public class LocalBinder extends Binder {

        public LiftService getService() {
            return LiftService.this;
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    if (device !=null) {
                        if (initSerialport(device)) {
                            Toast.makeText(arg0, "初始化串口成功", Toast.LENGTH_SHORT).show();
                            liftUtil.setSendListener(LiftService.this);

                            if (initListener != null) {
                                initListener.handleInit(true);
                            }
                            return;
                        }
                        else
                            Toast.makeText(arg0, "初始化串口失败", Toast.LENGTH_SHORT).show();

                    } else
                        Toast.makeText(arg0, "找不到USB转串口设备", Toast.LENGTH_SHORT).show();
                } else {   // User not accepted our USB connection. Send an Intent to the Main Activity

                }

                if (initListener != null) {
                    initListener.handleInit(false);
                }
            }


        }
    };

    private boolean initSerialport(UsbDevice device) {

        if (device == null)
            throw new NullPointerException("UsbDevice = null");

        connection = usbManager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(BAUD_RATE);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialPort.read(new UsbSerialInterface.UsbReadCallback() {
                    @Override
                    public void onReceivedData(final byte[] bytes) {

                        if (System.currentTimeMillis() - lastDataTime >= 50 && lastDataTime != 0) {
                            dataBuffer.flip();

                            final int length = dataBuffer.limit();
                            String data = "";
                            byte[] a = new byte[length];
                            for(int i=0; i<length; i++) {
                                a[i] = dataBuffer.get();
                                data += LiftUtil.byteToHexUnsignedString(a[i]);
                            }
                            System.out.println("receive data:"+data);
                            if (dataListener != null) {
                                dataListener.receive(a);
                            }

                            liftUtil.parse(a);
                            dataBuffer.clear();
                        }

                        if (bytes.length > dataBuffer.remaining())
                            return;

                        dataBuffer.put(bytes);
                        lastDataTime = System.currentTimeMillis();
                    }
                });

                return true;

            }
        }
        return false;
    }

    private UsbDevice findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice usbDevice = entry.getValue();
                int deviceVID = usbDevice.getVendorId();
                int devicePID = usbDevice.getProductId();

                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    return usbDevice;
                }
            }
        }
        return null;
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    private DataListener dataListener;

    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }

    public interface DataListener {
        void receive(byte[] data);
    }

    private InitListener initListener;

    public void setInitListener(InitListener initListener) {
        this.initListener = initListener;
    }

    public interface InitListener {
        void handleInit(boolean result);
    }
}
