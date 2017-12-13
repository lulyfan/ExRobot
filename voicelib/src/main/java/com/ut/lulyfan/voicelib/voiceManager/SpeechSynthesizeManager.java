package com.ut.lulyfan.voicelib.voiceManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.lang.ref.WeakReference;

/**
 * Created by acer on 2016/8/30.
 */
public class SpeechSynthesizeManager {

    private WeakReference<Context> wContext;
    private Toast toast;

    private static final int MODE_LOCAL = 0;
    private static final int MODE_CLOUD = 1;

    private SpeechSynthesizer mTts;
    // 默认云端发音人
//    private static final String voicerCloud="xiaoqi";
    private static final String voicerCloud="vinn";
    // 默认本地发音人
    private static final String voicerLocal="xiaoyan";

    private static String TAG = "SpeechSynthesizeManager";

    private Handler handler;

    public SpeechSynthesizeManager(Context context, boolean isOnline, Handler handler) {
        this(context, isOnline);
        this.handler = handler;
    }

    //isOnline:true:在线，false：离线模式
    public SpeechSynthesizeManager(Context context, boolean isOnline) {
        wContext = new WeakReference<Context>(context) ;
        mTts = SpeechSynthesizer.createSynthesizer(wContext.get(), mTtsInitListener);
        if (context != null && context instanceof Activity)
        {
            Activity activity = (Activity)context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast = Toast.makeText(wContext.get(), "", Toast.LENGTH_SHORT);
                }
            });
        }

        if (isOnline)
            setParam(MODE_CLOUD);    //设置参数
        else
            setParam(MODE_LOCAL);
    }

    /**
     * 开始语音合成
     * @param text：要进行合成的字符串
     * @return 合成失败返回false，否则返回true
     */
    public boolean startSpeaking(String text) {

        if (mTts.isSpeaking())
            mTts.stopSpeaking();

        int code = mTts.startSpeaking(text, mTtsListener);

        if (code != ErrorCode.SUCCESS) {
            Log.e(TAG, "语音合成失败,错误码: " + code);
            return false;
        }
        return true;
    }

    private boolean loopFlag;

    public void startSpeakingLoop(final String text, final long gapTime) {

        loopFlag = true;

        setSynCompletedListener(new SynCompletedListener() {
            @Override
            public void onSynCompleted(SpeechError error) {
                if (handler != null)
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (loopFlag)
                                startSpeaking(text);
                        }
                    }, gapTime);
            }
        });
        startSpeaking(text);
    }

    int speakedCount; //已经合成的次数
    public void startSpeakingMulti(final String text, final long gapTime, final int count) {

        if (count <= 0)
            return;

        speakedCount = 0;
        setSynCompletedListener(new SynCompletedListener() {
            @Override
            public void onSynCompleted(SpeechError error) {
                if (handler != null) {

                    if (speakedCount >= count) {
                        setSynCompletedListener(null);
                        return;
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startSpeaking(text);
                            speakedCount++;
                        }
                    }, gapTime);
                }
            }
        });
        startSpeaking(text);
        speakedCount++;
    }

    public void stopSpeakingLoop() {
        loopFlag = false;
        setSynCompletedListener(null);
        stopSpeaking();
    }

    public boolean isInSession() {
        return mTts.isSpeaking();
    }

    /**
     * 取消合成
     */
    public void stopSpeaking() {
        mTts.stopSpeaking();
    }

    /**
     * 暂停播放
     */
    public void pauseSpeaking() {
        mTts.pauseSpeaking();
    }

    /**
     * 继续播放
     */
    public void resumeSpeaking() {
        mTts.resumeSpeaking();
    }

    public void destory() {
        Log.i("destory", "destory tts...");
        if (mTts.isSpeaking())
            mTts.stopSpeaking();
        // 退出时释放连接
        boolean result = mTts.destroy();
        Log.i("destory", result?"destory tts success":"destory tts failed");
    }

    public void setVolume(String volume) {
        mTts.setParameter(SpeechConstant.VOLUME, volume);
    }

    //设置音调
    public void setPitch(String pitch) {
        mTts.setParameter(SpeechConstant.PITCH, pitch);
    }

    //设置语速
    public void setSpeed(String speed) {
        mTts.setParameter(SpeechConstant.SPEED, speed);
    }

    public void setVoicer(String voicer) {
        mTts.setParameter(SpeechConstant.VOICE_NAME,voicer);
    }

    /**
     * 参数设置
     * @param mode=0：本地参数设置，mode=1：在线参数设置
     */
    private void setParam(int mode) {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);

        if (mode == 0)
            setLocalParam();
        else if (mode == 1)
            setCloudParam();
        else
            showTip("无效模式");

        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
    }

    private void setCloudParam() {
        //设置在线引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME,voicerCloud);

    }

    private void setLocalParam() {
//        showTip("use local mode!");
        //设置使用本地引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
  /*      //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH,getResourcePath());
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME,voicerLocal);*/
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            showTip("语音合成引擎初始化"+(code == ErrorCode.SUCCESS ? "成功" : "失败"));
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败,错误码："+code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            if (handler != null)
                handler.sendEmptyMessage(VoiceConstant.MSG_SYN_SPEAKBEGIN);
            Log.i(TAG, "开始播放    合成状态:"+isInSession());
        }

        @Override
        public void onSpeakPaused() {
            Log.i(TAG, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            Log.i(TAG, "继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
//            mPercentForBuffering = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
//            mPercentForPlaying = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            Log.i(TAG, "syn onCompleted");
            if (handler != null)
                handler.sendEmptyMessage(VoiceConstant.MSG_SYN_SPEAKEND);

            if (synCompletedListener != null)
                synCompletedListener.onSynCompleted(error);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void showTip(final String str)
    {
        Activity activity = (Activity) wContext.get();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast.setText(str);
                toast.show();
            }
        });
    }

    //获取发音人资源路径
    private String getResourcePath(){
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(wContext.get(), ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(wContext.get(), ResourceUtil.RESOURCE_TYPE.assets, "tts/"+voicerLocal+".jet"));
        return tempBuffer.toString();
    }

    private SynCompletedListener synCompletedListener;
    public void setSynCompletedListener(SynCompletedListener listener) {
        synCompletedListener = listener;
    }
    public interface SynCompletedListener {
        void onSynCompleted(SpeechError error);
    }
}
