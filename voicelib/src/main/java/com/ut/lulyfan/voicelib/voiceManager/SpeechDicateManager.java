package com.ut.lulyfan.voicelib.voiceManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.UserWords;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by acer on 2016/11/10.
 */

public class SpeechDicateManager {
    private static String TAG = "SpeechDicateManager";
    private SpeechRecognizer mIat;
    private RecognizerDialog mIatDialog;
    private WeakReference<Context> wContext;
    private Toast toast;
    private String result = "";
    private boolean isIninted = false;
    private boolean isStoped = false;   //判断是都启动语音听写

    private Handler handler;

    public SpeechDicateManager(Context context, Handler handler) {
        this(context);
        this.handler = handler;
    }

    public SpeechDicateManager(Context context) {
        wContext = new WeakReference<Context>(context);
        if (context != null && context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast = Toast.makeText(wContext.get(), "", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    private void getLexicon(List<String> words) {
        UserWords orderWords = new UserWords();
        for (String word : words) {
            orderWords.putWord("robotWord", word);
        }

        int ret = mIat.updateLexicon("userword", orderWords.toString(), lexiconListener);
        if(ret != ErrorCode.SUCCESS){
            Log.i(TAG,"上传用户词表失败：" + ret);
        }
    }

    private LexiconListener lexiconListener = new LexiconListener() {
        @Override
        public void onLexiconUpdated(String lexiconId, SpeechError error) {
            if (error != null) {
                showTip("keyWord upload failed:+" + error.toString());
            } else {
                showTip("上传成功!");
            }
        }
    };

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
                isIninted = false;
                return;
            }
            setParam();
            isIninted = true;
            startDicate();
            Log.i("Dongni", "start dicate");
        }
    };

    public void initDicate() {
        isStoped = false;
        if (!isIninted) {
            mIat = SpeechRecognizer.createRecognizer(wContext.get(), mInitListener);
            setParam();
            isIninted = true;
            startDicate();
//            mIatDialog = new RecognizerDialog(wContext.get(), mInitListener);
//            mIatDialog.setListener(mRecognizerDialogListener);
        } else {
            startDicate();
        }
    }

    public void setStoped(boolean stoped) {
        isStoped = stoped;
    }

    public void startDicate() {
//        mIatDialog.show();
        if (!isStoped) {
            mIat.startListening(mRecoListener);
        }
    }

    public boolean isInSession() {
        return mIat.isListening();
    }

    private void setParam() {
        mIat.setParameter(SpeechConstant.PARAMS, null);
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        mIat.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
    }

    public void stopDicate() {
        mIat.stopListening();
    }

    public void destory() {
        if (mIat.isListening()) {
            mIat.cancel();
        }
        boolean result = mIat.destroy();
    }

    private RecognizerListener mRecoListener = new RecognizerListener() {
        //一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
        //isLast等于true时会话结束。
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String tmp = parseIatResult(results.getResultString());
            result += tmp;
            if (isLast) {
                if (handler != null) {
                    Message.obtain(handler, VoiceConstant.MSG_DICATE_RESULT, result).sendToTarget();
                }
                if (dicateListener != null) {
                    dicateListener.handleDicateResult(result, null);
                }
                result = "";
            }
        }

        @Override
        public void onError(SpeechError error) {
            if (dicateListener != null) {
                dicateListener.handleDicateResult(null, error);
            }
        }

        @Override
        public void onBeginOfSpeech() {
            if (handler != null) {
                Message.obtain(handler, VoiceConstant.MSG_DICATE_LISTENBEGIN).sendToTarget();
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            toast.setText("volume:"+volume);
            toast.show();
            if (handler != null) {
                Message.obtain(handler, VoiceConstant.MSG_DICATE_VOLUME, volume).sendToTarget();
            }
        }

        @Override
        public void onEndOfSpeech() {
            if (handler != null) {
                Message.obtain(handler, VoiceConstant.MSG_DICATE_LISTENEND).sendToTarget();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String tmp = parseIatResult(recognizerResult.getResultString());
            result += tmp;
            if (b && dicateListener != null) {
                Log.i(TAG, "dicateResult:" + result);
                dicateListener.handleDicateResult(result, null);
                result = "";
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.i(TAG, "error:" + speechError.getPlainDescription(true));
            if (dicateListener != null) {
                dicateListener.handleDicateResult(null, speechError);
            }
        }
    };

    private String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    private void showTip(final String str) {
        Activity activity = (Activity) wContext.get();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast.setText(str);
                toast.show();
            }
        });
    }

    private OnDicateListener dicateListener;

    public void setOnDicateListener(OnDicateListener listener) {
        dicateListener = listener;
    }

    public interface OnDicateListener {
        void handleDicateResult(String text, SpeechError error);
    }

}
