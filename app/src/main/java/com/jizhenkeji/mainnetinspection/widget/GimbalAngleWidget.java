package com.jizhenkeji.mainnetinspection.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import dji.common.gimbal.Attitude;
import dji.keysdk.GimbalKey;
import dji.keysdk.KeyManager;

/**
 * 大疆MSDK云台角度指示组件
 */
public class GimbalAngleWidget extends View {

    private Context mContext;

    /**
     * 默认组件宽度，单位：dp
     */
    private final int DEFAULT_WIDTH = 70;

    /**
     * 默认组件高度，单位：dp
     */
    private final int DEFAULT_HEIGHT = 30;

    public GimbalAngleWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
        initPaint();
        initGimbalAngleListener();
    }

    /**
     * 初始化组件视图
     */
    private void initView(){
        /* 隐藏组件 */
        setAlpha(0);
    }

    /**
     * 云台角度文本画笔
     */
    private Paint mTextPaint;

    /**
     * 初始化画笔
     */
    private void initPaint(){
        mTextPaint = new Paint();
        mTextPaint.setColor(0xFFFFFFFF);
    }

    /**
     * 初始化云台角度监听
     */
    private void initGimbalAngleListener(){
        GimbalKey gimbalKey = GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES);
        KeyManager keyManager = KeyManager.getInstance();
        keyManager.addListener(gimbalKey,(Object o, Object newValue) -> {
            if(newValue instanceof Attitude){
                Attitude attitude = (Attitude) newValue;
                if(mGimbalAngle == (int) attitude.getPitch()){
                    return;
                }
                mGimbalAngle = (int) attitude.getPitch();
                postInvalidate();
                mHandler.sendEmptyMessage(ACTIVATE_COMPONENT);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        if(widthMeasureMode == MeasureSpec.AT_MOST && heightMeasureMode == MeasureSpec.AT_MOST){
            setMeasuredDimension(GlobalUtils.dpToPx(DEFAULT_WIDTH), GlobalUtils.dpToPx(DEFAULT_HEIGHT));
        }else if(widthMeasureMode == MeasureSpec.AT_MOST){
            setMeasuredDimension(GlobalUtils.dpToPx(DEFAULT_WIDTH), heightMeasureSize);
        }else if(heightMeasureMode == MeasureSpec.AT_MOST){
            setMeasuredDimension(widthMeasureSize, GlobalUtils.dpToPx(DEFAULT_HEIGHT));
        }
    }

    /**
     * 激活云台角度显示组件标志位
     */
    private final int ACTIVATE_COMPONENT = 0x01;

    /**
     * 隐藏云台角度显示组件标志位
     */
    private final int HIDE_COMPONENT = 0x02;

    /**
     * 云台角度显示组件的显示、隐藏处理
     */
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

        /**
         * 隐藏组件动画
         */
        private ObjectAnimator mAnimator;

        /**
         * 激活组件显示，移除现有的隐藏消息，取消正在进行的动画，恢复组件至可见，发布新延时隐藏消息
         */
        private void activate(){
            mHandler.removeMessages(HIDE_COMPONENT);
            if(mAnimator != null){
                mAnimator.cancel();
            }
            setAlpha(1);
            mHandler.sendEmptyMessageDelayed(HIDE_COMPONENT, 1000);
        }

        /**
         * 隐藏组件
         */
        private void hide(){
            mAnimator = ObjectAnimator.ofFloat(GimbalAngleWidget.this, "alpha", 1f, 0f);
            mAnimator.setDuration(500);
            mAnimator.start();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case ACTIVATE_COMPONENT:
                    activate();
                    break;
                case HIDE_COMPONENT:
                    hide();
                    break;
            }
            return true;
        }
    });

    /**
     * 当前云台的角度
     */
    private int mGimbalAngle = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        /* 绘制图片 */
        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.radar);
        drawable.setBounds(0, 0, getWidth() / 2, getHeight());
        drawable.draw(canvas);
        /* 绘制文字 */
        mTextPaint.setTextSize(getHeight() * 2/ 3f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(mGimbalAngle), getWidth() * 4 / 5f, getHeight() * 3 / 4, mTextPaint);
    }

}
