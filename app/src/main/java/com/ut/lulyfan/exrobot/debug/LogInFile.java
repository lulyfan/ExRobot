package com.ut.lulyfan.exrobot.debug;

import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by acer on 2016/10/18.
 */

public class LogInFile {
    private final static String TAG = "LogInFile";
    private static String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/UTRobot/log.txt";

    public static boolean write(Object object) {
        return write(object.toString());
    }

    public static boolean write(String path, String str) {
        return write(path, str, true);
    }

    public synchronized static boolean write(String path, String str, boolean flag) {
        if (!isExternalStorageWritable())
            return false;

        try {
            FileOutputStream out = new FileOutputStream(path, true);
            out.write(str.getBytes());
            if (flag)
                out.write(("    time:"+DateFormat.getInstance().format(new Date())+"\n").getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "FileNotFoundException:::write "+str+"failed:"+e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "IOException:::write "+str+"failed:"+e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean write(String str) {
      return write(filePath, str);
    }

    public static void setPath(String path) {
        filePath = path;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return  Environment.MEDIA_MOUNTED.equals(state);

    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


}
