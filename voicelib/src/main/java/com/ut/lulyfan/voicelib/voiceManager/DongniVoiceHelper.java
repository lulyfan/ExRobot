package com.ut.lulyfan.voicelib.voiceManager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.ut.lulyfan.voicelib.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.ut.lulyfan.voicelib.voiceManager.DongniUnstand.TYPE_LEAD;
import static com.ut.lulyfan.voicelib.voiceManager.DongniUnstand.TYPE_SHOW;
import static com.ut.lulyfan.voicelib.voiceManager.DongniUnstand.TYPE_TALK;

/**
 * Created by acer on 2016/12/6.
 */

public class DongniVoiceHelper implements SpeechDicateManager.OnDicateListener, SpeechSynthesizeManager.SynCompletedListener, DongniUnstand.OnTextUnderstandListener, VoiceView.OnTouchListener {
    private DongniUnstand understand;
    private SpeechDicateManager speechDicateManager;
    private static SpeechSynthesizeManager speechSynthesizeManager;

    private int state = INACTIVE;
    private static final int INACTIVE = -1;
    private static final int LISTENING = 0;
    private static final int RECOGNIZING = 1;
    private static final int SPEAKING = 2;

    Context context;

    private Handler handler;

    private VoiceView voiceView;
    private TextView tipView;

    private long lastTime;

    private Timer errorTimer;

    private boolean isInited = false;
    private boolean isStoped = false;

    private UTRobot robot;

    private String dicateText;

    private static DongniVoiceHelper voiceHelper;
    private boolean isCommonSyn;   //是否为普通语音合成

    public static synchronized DongniVoiceHelper getInstance() {
        return voiceHelper;
    }

    public static synchronized DongniVoiceHelper getInstance(Context context, Handler handler, ViewGroup parent, UTRobot robot) {
        if (voiceHelper == null)
            voiceHelper = new DongniVoiceHelper(context, handler, parent, robot);
        return voiceHelper;
    }

    public DongniVoiceHelper(Context context, Handler handler, ViewGroup parent, UTRobot robot) {

        this.context = context;

        this.robot = robot;
        understand = new DongniUnstand(robot, myHandler);
        understand.setOnTextUnderstandListener(this);

        speechDicateManager = new SpeechDicateManager(context, myHandler);
        speechDicateManager.setOnDicateListener(this);
        speechSynthesizeManager = new SpeechSynthesizeManager(context, false, myHandler);
        speechSynthesizeManager.setSynCompletedListener(this);

        View view = ((Activity) context).getLayoutInflater().inflate(R.layout.voice_view, parent);
        voiceView = (VoiceView) view.findViewById(R.id.voiceView);
        voiceView.setHandleTouchable(true);
        tipView = (TextView) view.findViewById(R.id.tv_tip);
        voiceView.setOnTouchListener(this);

        this.handler = handler;
    }

    public static void loadVoice(Context context) {
        StringBuffer param = new StringBuffer();
        param.append("appid=578c80cd");
        param.append(",");
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_AUTO);
        SpeechUtility.createUtility(context, param.toString());
    }

    public static boolean isHasInstance() {
        return voiceHelper == null ? false : true;
    }

    public void setVoiceViewAndHandle(Context context, ViewGroup parent, Handler handler) {
        setVoiceView(context, parent);
        this.handler = handler;
    }

    public void setVoiceView(Context context, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.voice_view, parent);
        voiceView = (VoiceView) view.findViewById(R.id.voiceView);
        voiceView.setHandleTouchable(true);
        tipView = (TextView) view.findViewById(R.id.tv_tip);
        voiceView.setOnTouchListener(this);
    }

    public void setVoiceView(VoiceView voiceView, TextView tipView) {
        this.voiceView = voiceView;
        this.tipView = tipView;

        voiceView.setHandleTouchable(true);
        voiceView.setOnTouchListener(this);
    }

    public VoiceView getVoiceView() {
        return voiceView;
    }

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {

                case VoiceConstant.MSG_SYN_SPEAKBEGIN:        //语音合成开始消息
                    state = SPEAKING;
                    tipView.append("\n语音播放中...");
                    tipView.append("\nPS:点击图标可关闭语音播放");
                    break;

                case VoiceConstant.MSG_DICATE_LISTENBEGIN:   //语音听写开始消息
                    state = LISTENING;
                    tipView.setText("倾听中");
                    voiceView.setState(0);
                    break;

                case VoiceConstant.MSG_DICATE_VOLUME:         //语音听写音量变化消息
                    long gapTime = System.currentTimeMillis() - lastTime;
                    if (gapTime < 200)
                        return;
                    int volume = 0;
                    if (msg.obj != null)
                        volume = (int)(msg.obj);
                    if (volume > 0)
                        volume = volume / 4 + 1;
                    voiceView.setVolume(volume);
                    lastTime = System.currentTimeMillis();
                    break;

                case VoiceConstant.MSG_DICATE_LISTENEND:     //语音听写结束消息
                    state = RECOGNIZING;
                    tipView.setText("正在处理中,请稍候...");
                    voiceView.setState(1);
                    break;

                case VoiceConstant.MSG_SET_SCENE:
                    String result = (String) msg.obj;
                    if (result.equals("success")) {
                        isInited = true;
                        startVoice();
                    }
                    else
                        tipView.setText(result+"\n语音初始化失败,点击语音图标重试");
                    break;
            }
        }
    };

    @Override
    public void handleDicateResult(final String text, SpeechError error) {
        if (error != null) {
            handleError(error.getPlainDescription(true));
            return;
        }
        tipView.append("\n识别结果:"+text);
        dicateText = text;
        if (!localUnderstand(text))
            understand.understand(text);
    }

    private boolean localUnderstand(String text) {

        boolean flag = false;

        if (text.contains("欢迎光临")) {
            speechSynthesizeManager.startSpeaking("刘先生您好！欢迎光临。");
            flag = true;
        }
        else if (text.contains("优惠")) {
            speechSynthesizeManager.startSpeaking("刘先生，您的房间号是215，另外您今天生日，所以今天会给您9折优惠");
            flag = true;
        }
        else if (text.contains("跟我来")) {
            speechSynthesizeManager.startSpeaking("刘先生您好，您的215房间在这边，请跟我来。");
            flag = true;
        }
        else if (text.contains("生日")) {
            speechSynthesizeManager.startSpeaking("刘先生，祝您生日快乐！");
            flag = true;
        }
        else if (text.contains("到达")) {
            speechSynthesizeManager.startSpeaking("您的包厢已到达，祝您消费愉快。");
            flag = true;
        }
        else if (text.contains("配送")) {
            speechSynthesizeManager.startSpeaking("203号、215号包厢有餐食待送，请优小妹到吧台配送。");
            flag = true;
        }
        else if (text.contains("领取")) {
            speechSynthesizeManager.startSpeaking("2015号包厢餐食已送达，请注意领取");
            flag = true;
        }

        return flag;
    }

    public void speak(String text) {
        isCommonSyn = true;
        speechSynthesizeManager.startSpeaking(text);
    }

    @Override
    public void onSynCompleted(SpeechError error) {
        if (error != null) {
            handleError("\n" + error.getPlainDescription(true));
            return;
        }

        if (isCommonSyn) {
            isCommonSyn = false;
            return;
        }

        tipView.setText(tipView.getText().toString().replace("\n语音播放中...\nPS:点击图标可关闭语音播放", ""));
        if (!isStoped)
            speechDicateManager.startDicate();
    }

    @Override
    public void handleTextUnderstandResult(String text, String error) {
        if (text == null) {
            handleError(error);
            return;
        }

        Log.i("dongni", "dongni json text:"+text);

        try {
            JSONObject root = new JSONObject(text);
            int code = root.getInt("code");
            if (code != 200) {
                handleError("懂你:"+text);
                return;
            }
            JSONObject data = root.getJSONObject("data");
            int type = data.getInt("type");
            JSONArray content = data.getJSONArray("content");
            switch (type) {

                case TYPE_TALK:
//                    Toast.makeText(context, "懂你:"+content.getString(0), Toast.LENGTH_SHORT).show();
                    tipView.append("\n懂你:"+content.getString(0));
                    Message.obtain(handler, VoiceConstant.MSG_TALK, "懂你:"+content.getString(0)).sendToTarget();
                    speechSynthesizeManager.startSpeaking(content.getString(0));
                    break;

                case TYPE_LEAD:
                    if (content.length() == 1) {
                        JSONObject item0 = content.getJSONObject(0);
                        int categoryNum = item0.getInt("categoryNum");

                        String dongniDest = robot.getScene();
                        for (int i=1; i<=categoryNum; i++) {
                            String temp =  item0.getString("category"+i);
                            if (temp.equals(""))
                                temp = "综合";
                            dongniDest += ":"+temp;
                        }

                        String scene = robot.getScene();
                        if (scene.contains("GMO")) {
                            Message.obtain(handler, VoiceConstant.MSG_DEST, dongniDest).sendToTarget();
                        } else if (scene.contains("KTV")) {

                        }
                    } else if (content.length() > 1) {
                        String intentCategoty = data.getJSONArray("matchList").getJSONObject(0).getString("type");
                        String intentContent = data.getJSONArray("matchList").getJSONObject(0).getString("content");

                        HashMap<String, ArrayList> map = new HashMap<>();
                        ArrayList<String> titleData = new ArrayList<>();
                        ArrayList<String> detailData = new ArrayList<>();
                        titleData.add(intentContent);

                        for (int i=0; i<content.length(); i++) {
                            JSONObject item = content.getJSONObject(i);
                            int categoryNum = item.getInt("categoryNum");
                            for (int j=1; j<=categoryNum; j++) {
                                String category = "category"+j;
                                if (!category.equals(intentCategoty))
                                    detailData.add(item.getString(category));
                            }
                        }

                        map.put("title", titleData);
                        map.put("detail", detailData);
                        Message.obtain(handler, VoiceConstant.MSG_MUTIL_DEST, map).sendToTarget();

                    }
//                    JSONObject item0 = content.getJSONObject(0);
//                    String dest = item0.getString("category1");
//                    String rooms[] = ((SpeechApp)(context.getApplicationContext())).ROOMS;
//
//                    if (!isContainRoom(rooms, dest)) {
//                        if (robot.getFloor() == "2") {
//                            dest = "电梯口";
//                            isStoped = true;
//                            speechSynthesizeManager.startSpeaking("您的目的地不在本楼层,需要带您到电梯口吗?请在屏幕上点击确认或取消");
//                        } else {
//                            speechSynthesizeManager.startSpeaking("您的目的地不在本楼层");
//                            return;
//                        }
//
//                    } else {
//                        isStoped = true;
//                        speechSynthesizeManager.startSpeaking("您的目的地为"+dest+",请在屏幕上点击确认或取消");
//                    }
//                    Message.obtain(handler, VoiceConstant.MSG_DEST, dest).sendToTarget();
                    break;

                case TYPE_SHOW:
                    List<Goods> goodsList = new ArrayList<>();
                    for (int i=0; i<content.length(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        /**
                         * ...进行json解析，待定
                         */
                        Message.obtain(handler, 101, goodsList).sendToTarget();
                    }
                    break;

//                -default:
//                    textUnderstandManager.startUnderstanding(dicateText);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleError(String error) {
        voiceView.setState(4);
        tipView.append("\n"+error);
        errorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                speechDicateManager.startDicate();
            }
        }, 2000);
    }

    //判断该房间是否为本楼层的
    private boolean isContainRoom(String rooms[], String room) {
        for (int i=0; i<rooms.length; i++) {
            if (rooms[i] == null)
                continue;
            if (room.equals(rooms[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUp() {
        if (state == SPEAKING)
            speechDicateManager.startDicate();
    }

    @Override
    public void onDown() {
        if (state == INACTIVE)
            initVoice();
        else if (state == SPEAKING) {
            speechSynthesizeManager.stopSpeaking();
            voiceView.setState(3);
        }
    }

    public void pauseVoice() {
        pauseVoice(true);
    }

    //pauseSyn: true: 暂停合成   false:不暂停合成
    public void pauseVoice(boolean pauseSyn) {
        if (!isInited)
            return;

        if (pauseSyn) {
            if (speechSynthesizeManager.isInSession())
                speechSynthesizeManager.stopSpeaking();
        }

        if (speechDicateManager.isInSession())
            speechDicateManager.stopDicate();
        speechDicateManager.setStoped(true);
        voiceView.cancelRecognize();

//        if (errorTimer != null) {
//            errorTimer.cancel();
//            errorTimer = null;
//        }
    }

    public void initVoice() {
        isStoped = false;
        if (isInited) {
            startVoice();
            return;
        }
        tipView.setText("语音正在初始化中,请稍候...");
        understand.asyncSetScene();
    }

    public void startVoice() {
        if (errorTimer == null)
            errorTimer = new Timer();
        speechDicateManager.initDicate();
        voiceView.setEnableTimer(true);
    }

    public void destory() {
        voiceView.cancelRecognize();
        speechSynthesizeManager.destory();
        speechDicateManager.destory();
        voiceHelper = null;
    }

    static class Goods {

    }
}
