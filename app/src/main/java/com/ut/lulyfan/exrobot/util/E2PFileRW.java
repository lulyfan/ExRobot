package com.ut.lulyfan.exrobot.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

public class E2PFileRW {

	private static String file_name="/dev/block/platform/sdhci-tegra.3/by-name/E2P";
	private static String TAG="E2PFileRW";
	static File file=null;
	public static void initDevice(){
		file=new File(file_name);
		if(!file.exists()){
			Log.i(TAG,file_name + "not exists");
		}
	}
	
	public static void closeDevice(){
	}
	
	public static String getMacAddress(){
		return read(9,15);
	}
	
	public static String getMcidNumber(){
		return read(1024,1040);
	}
	
	public  static boolean setMacAddress(String maddr){
		return write(maddr, 9);
	}
	public  static boolean setMcidNumber(String mmcid){
		return write(mmcid,1024);
	}
	
	public static String read(int from ,int to){
        String result="";
        try{
            FileInputStream fis=new FileInputStream(file);
            BufferedInputStream bis=new BufferedInputStream(fis);
            bis.skip(from);
            int c=0;
            for(int i=0;(i<to-from)&&(c=bis.read())!=-1;i++){
            	if(c>122||c<=0){
            		continue;
            	}else{
            		result+=(char)c;
					System.out.println("vallen "+c);
            	}
            }
            bis.close();
            fis.close();
        }catch(Exception e){
        	e.printStackTrace();
        }
        return result;
    }
	
	public static boolean write(String str,int pos){
		boolean ok=false;

	    try {
	        RandomAccessFile raf = new RandomAccessFile(file,"rw");
	        byte[] b = str.getBytes();
	        raf.seek(pos);
	        raf.write(b, 0, b.length);
	        raf.close();
	        ok=true;
	    } catch (Exception e) {
	    	Log.e(TAG,e.getMessage());
	    	ok=false;
	    }
	    return ok;
	}

}
