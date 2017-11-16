package com.ut.lulyfan.voicelib.voiceManager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.ut.lulyfan.voicelib.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by acer on 2016/12/1.
 */

public class VoiceView extends View {
    private static final String TAG = "VoiceView";
    private int state = INACTIVE;
    private static final int INACTIVE = -1;
    private static final int LISTENING = 0;
    private static final int RECOGNIZING = 1;
    private static final int SPEAKING = 2;
    private static final int CANCEL = 3;
    private static final int ERROR = 4;

    private float s ;   //比例因子，控制伸缩
    private int mFirstColor;
    private int mSecondColor;
    private int mCircleWidth;
    private Paint mPaint;
    private int mCurrentCount;
//    private Bitmap mImage;
    private int mSplitSize;
    private int mCount;
    private Rect mRect;
    private Timer timer;

    public void setEnableTimer(boolean enableTimer) {
        isEnableTimer = enableTimer;
    }

    private boolean isEnableTimer = false;   //是否使能定时器
    private boolean isHandleTouch = false; //是否处理侦听事件
    private int mProgress;
    public VoiceView(Context context) {
        this(context, null);
    }

    public VoiceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VoiceView, defStyleAttr, 0);
        int n = a.getIndexCount();

        for (int i=0; i<n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.VoiceView_firstColor) {
                mFirstColor = a.getColor(attr, Color.GRAY);
            } else if (attr == R.styleable.VoiceView_secondColor) {
                mSecondColor = a.getColor(attr, Color.BLUE);
            } else if (attr == R.styleable.VoiceView_circleWidth) {
                mCircleWidth = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 20, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.VoiceView_dotCount) {
                mCount = a.getInt(attr, 20);
            } else if (attr == R.styleable.VoiceView_splitSize) {
                mSplitSize = a.getInt(attr, 20);
            }
//            } else if (attr == R.styleable.VoiceView_bg) {
//                mImage = BitmapFactory.decodeResource(getResources(), a.getResourceId(attr, 0));
            else if (attr == R.styleable.VoiceView_currentCount) {
                mCurrentCount = a.getInt(attr, 0);
            }
        }
        a.recycle();
        mPaint = new Paint();
        mRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        s = 1.0f / 120 * getWidth();
        switch (state) {
            case INACTIVE:
                drawRecord(canvas, getWidth() / 2);
                break;
            case LISTENING:
                drawListening(canvas);
                break;
            case RECOGNIZING:
                drawRecognizing(canvas);
                break;
            case SPEAKING:
                drawSpeaking(canvas);
                break;
            case CANCEL:
                drawCancel(canvas);
                break;
            case ERROR:
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.warning);
                drawBitmap(canvas, bitmap);
        }
    }

    private void drawSpeaking(Canvas canvas) {
        drawWaiting(canvas);
    }

    private void drawCancel(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 61, 169, 237);
        canvas.drawArc(new RectF(0, 0, getWidth(), getHeight()), 0, 360, false, mPaint);

        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(10 * s);
        int center = getWidth() / 2;
        float length = (float)(2.0f / 3 * getWidth() / 2 * Math.sqrt(2) / 2);
        float x1 = center - length, y1 = center - length;
        float x2 = center + length, y2 = center + length;
        canvas.drawLine(x1, y1, x2, y2, mPaint);

        y1 = center + length;
        y2 = center - length;
        canvas.drawLine(x1, y1, x2, y2, mPaint);
    }

    private void drawRecognizing(Canvas canvas) {
        drawWaiting(canvas);
    }

    private void drawWaiting(Canvas canvas) {
        int count = 5, splitSize = 5, strokeWidth = 4;
        int itemSize = 360 / count - splitSize;
        int centre = getWidth() / 2;
        int radius = centre - strokeWidth / 2;
        mPaint.reset();
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 61, 169, 237);
        RectF oval = new RectF(centre - radius, centre - radius, centre + radius, centre + radius);
        for (int i = 0; i < mCount; i++)
        {
            canvas.drawArc(oval, i * (itemSize + splitSize) + mProgress, itemSize, false, mPaint); // 根据进度画圆弧
        }

        drawRecord(canvas, getWidth() / 2);

        if (timer == null && isEnableTimer) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //Log.i(TAG, "timer running...");
                    mProgress++;
                    if (mProgress == 360)
                        mProgress = 0;
                    postInvalidate();
                }
            }, 0, 10);
            System.out.println("create timer!!!");
        }
    }

    public void cancelRecognize() {
        if (timer != null) {
            isEnableTimer = false;
            timer.cancel();
            timer = null;

            System.out.println("cancel timer!!!");
        }
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap) {
        int centre = getWidth() / 2; // 获取圆心的x坐标
        int radius = centre - mCircleWidth / 2;// 半径
        /**
         * 计算内切正方形的位置
         */
        int relRadius = radius - mCircleWidth / 2;// 获得内圆的半径
        /**
         * 内切正方形的距离顶部 = mCircleWidth + relRadius - √2 / 2
         */
        mRect.left = (int) (relRadius - Math.sqrt(2) * 1.0f / 2 * relRadius) + mCircleWidth;
        /**
         * 内切正方形的距离左边 = mCircleWidth + relRadius - √2 / 2
         */
        mRect.top = (int) (relRadius - Math.sqrt(2) * 1.0f / 2 * relRadius) + mCircleWidth;
        mRect.bottom = (int) (mRect.left + Math.sqrt(2) * relRadius);
        mRect.right = (int) (mRect.left + Math.sqrt(2) * relRadius);

        /**
         * 如果图片比较小，那么根据图片的尺寸放置到正中心
         */
        if (bitmap.getWidth() < Math.sqrt(2) * relRadius)
        {
            mRect.left = (int) (mRect.left + Math.sqrt(2) * relRadius * 1.0f / 2 - bitmap.getWidth() * 1.0f / 2);
            mRect.top = (int) (mRect.top + Math.sqrt(2) * relRadius * 1.0f / 2 - bitmap.getHeight() * 1.0f / 2);
            mRect.right = (int) (mRect.left + bitmap.getWidth());
            mRect.bottom = (int) (mRect.top + bitmap.getHeight());

        }
        // 绘图
        canvas.drawBitmap(bitmap, null, mRect, mPaint);
    }

    private void drawRecord(Canvas canvas, int centre) {
        mPaint.reset();
        mPaint.setAntiAlias(true); // 消除锯齿
        mPaint.setStrokeWidth(mCircleWidth); // 设置圆环的宽度
        mPaint.setStrokeCap(Paint.Cap.ROUND); // 定义线段断电形状为圆头
        mPaint.setAntiAlias(true); // 消除锯齿
        mPaint.setStyle(Paint.Style.STROKE); // 设置空心

        //head
        mPaint.reset();
        mPaint.setAntiAlias(true);
        if (state == INACTIVE)
            mPaint.setColor(Color.GRAY);
        else
            mPaint.setARGB(255, 61, 169, 237);
        float width=30 * s;
        float height=width / 2;
        float x1=centre-width/2, y1=centre-height/2;
        float x2=centre+width/2, y2=centre+height/2;
        Path path = new Path();
        path.addArc(new RectF(x1, y1-width/2, x2, y1+width/2), 180, 180);
        path.lineTo(x2, y2);
        path.arcTo(new RectF(x1, y2-width/2, x2, y2+width/2), 0, 180);
        path.lineTo(x1, y1);
        path.close();
        canvas.drawPath(path, mPaint);

        //middle
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5 * s);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        float gap = 5 * s;
        float r1 = gap + width/2;
        float x_cp = centre, y_cp = centre + height/2;
        canvas.drawArc(new RectF(x_cp - r1, y_cp - r1, x_cp + r1, y_cp + r1), 0, 180, false, mPaint);

        //last
        float length = 10 * s;
        canvas.drawLine(x_cp, y_cp + r1, x_cp, y_cp + r1 + length, mPaint);
        canvas.drawLine(x1, y_cp + r1 + length, x2, y_cp + r1 + length, mPaint);
    }

    private void drawListening(Canvas canvas) {
        int centre = getWidth() / 2; // 获取圆心的x坐标
        int radius = centre - mCircleWidth / 2;// 半径
        drawRecord(canvas, centre);
        drawOval(canvas, centre, radius);
    }

    private void drawOval(Canvas canvas, int centre, int radius)
    {
        /**
         * 根据需要画的个数以及间隙计算每个块块所占的比例*360
         */
//        float itemSize = (360 * 1.0f - mCount * mSplitSize) / mCount;
        float itemSize = (180f + 25f + 25f - (mCount - 1) * mSplitSize) / mCount;

        RectF oval = new RectF(centre - radius, centre - radius, centre + radius, centre + radius); // 用于定义的圆弧的形状和大小的界限

        mPaint.setAlpha(255 * 50 / 100);
        mPaint.setColor(mFirstColor); // 设置圆环的颜色
        for (int i = 0; i < mCount; i++)
        {
            canvas.drawArc(oval, i * (itemSize + mSplitSize) + 155, itemSize, false, mPaint); // 根据进度画圆弧
        }

        mPaint.setColor(mSecondColor); // 设置圆环的颜色
        for (int i = 0; i < mCurrentCount; i++)
        {
            canvas.drawArc(oval, i * (itemSize + mSplitSize) + 155, itemSize, false, mPaint); // 根据进度画圆弧
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "event:"+event.getAction());
        if (!isHandleTouch)
            return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (listener != null)
                listener.onDown();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (listener != null)
                listener.onUp();
        }
        return true;
    }

    public void up()
    {
        mCurrentCount++;
        postInvalidate();
    }

    public void down()
    {
        mCurrentCount--;
        postInvalidate();
    }

    public void setState(int state) {
        this.state = state;
        invalidate();
    }

    public void setHandleTouchable(boolean flag) {
        isHandleTouch = flag;
    }

    public void setVolume(int n) {
        state = LISTENING;
        if (n >= 0 && n <= 6 && mCurrentCount != n) {
            mCurrentCount = n;
            invalidate();
        } else if (n > 6 && mCurrentCount != n) {
            mCurrentCount = 6;
            invalidate();
        }
    }


    private OnTouchListener listener;
    public void setOnTouchListener(OnTouchListener listener) {
        this.listener = listener;
    }
    public interface OnTouchListener {
        void onUp();
        void onDown();
    }
}
