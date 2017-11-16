package com.ut.lulyfan.exrobot.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by acer on 2017/1/17.
 */

public class DotView extends View {

    private ShapeDrawable[] dots;
    private int count = 5;
    private int dotDia = 20; //圆点直径
    private int select;
    private Timer timer;

    public DotView(Context context) {
        super(context);
    }

    public DotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        dots = new ShapeDrawable[count];
        Rect rect = new Rect(10, 10, 10 + dotDia, 10 + dotDia);

        for (int i=0; i<dots.length; i++) {

            dots[i] = new ShapeDrawable(new OvalShape());
            dots[i].setBounds(rect);

            rect.left = rect.right + 20;
            rect.right = rect.left + dotDia;
        }
    }

    public DotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        if (wSpecMode == MeasureSpec.AT_MOST)
            wSize = 20 * 5 + 20 * 4 + 10;

        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        if (hSpecMode == MeasureSpec.AT_MOST)
            hSize = 10 + 20;

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(wSize, wSpecMode);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(hSize, hSpecMode);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.i("dotView", "measure width:"+getMeasuredWidth());
        Log.i("dotView", "width:"+getWidth());
        Log.i("dotView", "min width:"+getMinimumWidth());

        for (int i=0; i<dots.length; i++) {
            if (i == select)
                dots[i].getPaint().setColor(Color.RED);
            else
                dots[i].getPaint().setColor(Color.BLUE);
        }

        for (ShapeDrawable drawable : dots)
            drawable.draw(canvas);
    }

    public void select(int select) {
        this.select = select;
        postInvalidate();
    }

    public void flow(long delay) {
        if (timer == null)
            timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                select++;
                if (select >= count)
                    select = 0;

                select(select);
            }
        }, 0, delay);
    }

    public void clear() {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }
}
