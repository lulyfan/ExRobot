package com.ut.lulyfan.voicelib.voiceManager;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by acer on 2016/11/22.
 */
public class DongniUnstand {
    //    private static final String url = "www.baidu.com";
    private static final String url = "54.223.224.104";
    private static final String userId = "584f9fb23cb14c284873d57c";

    //语义分析接口的type字段:
    public static final int TYPE_TALK = 1;
    public static final int TYPE_LEAD = 2;
    public static final int TYPE_SHOW = 3;    //屏幕展示

    //结果反馈接口中的intent字段
    public static final int INTENT_LEAD = 1;
    public static final int INTENT_TALK = 2;

    //结果反馈中的operate字段
    public static final int OPERATE_TRUE = 1;    //表示和预测结果一致
    public static final int OPERATE_FALSE = 2;    //不一致

    IUnderstand understand;
    ISetScene setScene;
    ISetResult setResult;
    IGetRobotIP getRobotIP;

    ExecutorService service;

    Handler handler;

    UTRobot robot;

    public DongniUnstand(UTRobot robot, Handler handler) {
        this(robot);
        this.handler = handler;
    }

    public DongniUnstand(UTRobot robot) {

        this.robot = robot;

        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://"+url+":5100")
                .baseUrl("http://"+url+":5500")    //测试
                .build();

        understand = retrofit.create(IUnderstand.class);
        setResult = retrofit.create(ISetResult.class);
        setScene = retrofit.create(ISetScene.class);

        service = Executors.newCachedThreadPool();
    }

    public DongniUnstand(Handler handler, String url, int port) {
        service = Executors.newCachedThreadPool();
        this.handler = handler;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://"+url+":"+port)
                .build();
        getRobotIP = retrofit.create(IGetRobotIP.class);
    }

    public void understand(final String text) {

        Call<ResponseBody> call = understand.getUnderstand(userId, robot.getSn(), text);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String result = null;
                if (response != null) {
                    try {

                        result = response.body().string();
                        Log.i("Dongni","success:"+result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (textUnderstandListener != null) {
                    textUnderstandListener.handleTextUnderstandResult(result, null);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("Dongni", "failed:"+t.getMessage());
                if (textUnderstandListener != null) {
                    textUnderstandListener.handleTextUnderstandResult(null, "failed connect to Dongni:" + t.getMessage());
                }
            }
        });
    }

    public void asyncSetScene() {
        asyncSetScene(robot.getScene());
    }

    public void asyncSetScene(final String scene) {
        service.execute(new Runnable() {
            @Override
            public void run() {
                String result = setScene(scene);
                if (handler != null) {
                    Message.obtain(handler, VoiceConstant.MSG_SET_SCENE, result).sendToTarget();
                }
                Log.i("Dongni", "Dongni set scene result:"+result);
            }
        });
    }

    public void asyncSetResult(final int intent, final int operate) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                String result = setResult(intent, operate);
                Log.i("Dongni", "Dongni report result:"+result);
            }
        });
    }

    public String setScene(String scene) {
        Call<ResponseBody> call = setScene.setScene(robot.getSn(), scene);
        try {
            Response<ResponseBody> response = call.execute();
            if (response.body() == null) {
                return "response = null";
            }
            String str = response.body().string();
            JSONObject root = new JSONObject(str);
            int code = root.getInt("code");
            if (code == 200) {
                return "success";
            }
            String message = root.getString("message");
            return code+":"+message;

        } catch (IOException e) {
            e.printStackTrace();
            return "登录失败,网络连接异常";
        } catch (JSONException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * 结果反馈
     * @param intent: 懂你对用户意图的猜测 1:带路 2:对话
     * @param operate 是否和预测结果一致 1:一致 2:不一致
     * @return code+message
     */
    public String setResult(int intent, int operate) {
        Call<ResponseBody> call = setResult.setResult(robot.getSn(), intent, operate);
        try {
            Response<ResponseBody> response = call.execute();
            String str = response.body().string();
            JSONObject root = new JSONObject(str);
            int code = root.getInt("code");
            if (code == 200) {
                return "success";
            }
            String message = root.getString("message");
            return code+":"+message;
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (JSONException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public void asyncGetRobotIp() {
        service.execute(new Runnable() {
            @Override
            public void run() {
                String result = getRobotIP();
                if (handler != null) {
                    Message.obtain(handler, VoiceConstant.MSG_GET_ROBOT_IP, result).sendToTarget();
                }
                Log.i("Dongni", "Robot IP:"+result);
            }
        });
    }

    public String getRobotIP() {
        Call<ResponseBody> call = getRobotIP.getRobotIp();
        try {
            Response<ResponseBody> response = call.execute();
            String str = response.body().string();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private OnTextUnderstandListener textUnderstandListener;
    public void setOnTextUnderstandListener(OnTextUnderstandListener listener)
    {
        textUnderstandListener = listener;
    }
    public interface OnTextUnderstandListener {
        void handleTextUnderstandResult(String text, String error);
    }

    interface IUnderstand {
        @GET("api/lanAnalysis")
        Call<ResponseBody> getUnderstand(@Query("userId") String userId, @Query("customerId") String customerId, @Query("msg") String msg);
    }

    interface ISetScene {
        @FormUrlEncoded
        @POST("api/robotLogin")
        Call<ResponseBody> setScene(@Field("customerId") String custormerId, @Field("scene") String scene);
    }

    interface ISetResult {
        @GET("api/resultCollect")
        Call<ResponseBody> setResult(@Query("customerId") String customerId, @Query("intent") int intent, @Query("operate") int operate);
    }

    interface IGetRobotIP {
        @GET("RobotService/GetRobotIp")
        Call<ResponseBody> getRobotIp();
    }
}

