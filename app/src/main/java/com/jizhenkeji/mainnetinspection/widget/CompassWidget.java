package com.jizhenkeji.mainnetinspection.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;

/**
 * 无人机偏航角指示罗盘组件
 */
public class CompassWidget extends View {

    /**
     * 罗盘组件默认宽度
     */
    private final int DEFAULT_WIDTH = 500;

    /**
     * 罗盘组件默认高度
     */
    private final int DEFAULT_HEIGHT = 150;

    /**
     * 罗盘刻度显示范围
     */
    private final int ANGLE_RANGE = 45;

    /**
     * 罗盘箭头绘制画笔
     */
    private Paint mArrowPaint;

    /**
     * 罗盘刻度绘制画笔
     */
    private Paint mScalePaint;

    /**
     * 罗盘文本绘制画笔
     */
    private Paint mScaleTextPaint;

    /**
     * 当前飞机偏航角
     */
    private int mCurrentHeading = 0;

    public CompassWidget(Context context) {
        super(context, null);
    }

    public CompassWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCompassHeadingListener();

        mArrowPaint = new Paint();
        mScaleTextPaint = new Paint();
        mScalePaint = new Paint();
    }

    /**
     * 初始化机头方向监听
     */
    private void initCompassHeadingListener(){
        KeyManager keyManager = KeyManager.getInstance();
        FlightControllerKey compassHeadingKey = FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING);
        keyManager.addListener(compassHeadingKey, (Object oldValue, Object newValue) -> {
            if(newValue instanceof Float){
                float heading = (float) newValue;
                if(mCurrentHeading != (int)heading){
                    this.mCurrentHeading = (int)heading;
                    postInvalidate();
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /* 测量组件宽度 */
        int widthSize = DEFAULT_WIDTH;
        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        switch (widthMeasureMode) {
            case MeasureSpec.AT_MOST:
                widthSize = widthSize > widthMeasureSize ? widthMeasureSize : widthSize;
                break;
            case MeasureSpec.EXACTLY:
                widthSize = widthMeasureSize;
                break;
        }
        /* 测量组件高度 */
        int heightSize = DEFAULT_HEIGHT;
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        switch (heightMeasureMode) {
            case MeasureSpec.AT_MOST:
                heightSize = heightSize > heightMeasureSize ? heightMeasureSize : heightSize;
                break;
            case MeasureSpec.EXACTLY:
                heightSize = heightMeasureSize;
                break;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * 初始化画笔属性
     * @param canvas
     */
    private void initPaint(Canvas canvas){
        mArrowPaint.setColor(0xFFFFFFFF);
        mArrowPaint.setStyle(Style.FILL);
        /* 创建渐变阴影 */
        Shader mShader = new LinearGradient(0,canvas.getHeight() / 2,canvas.getWidth(),canvas.getHeight() / 2,
                new int[] {0x22FFFFFF, 0xFFFFFFFF, 0x22FFFFFF},null,Shader.TileMode.CLAMP);
        mScaleTextPaint.setStyle(Style.FILL);
        mScaleTextPaint.setTextAlign(Paint.Align.CENTER);
        mScaleTextPaint.setFakeBoldText(true);
        mScaleTextPaint.setTextSize(canvas.getHeight() / 3);
        mScaleTextPaint.setShader(mShader);

        mScalePaint.setStyle(Style.STROKE);
        mScalePaint.setStrokeWidth(canvas.getHeight() / 15);
        mScalePaint.setShader(mShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /* 初始化画笔 */
        initPaint(canvas);
        /* 开始绘制罗盘 */
        onDrawArrow(canvas);        // 绘制顶部箭头
        onDrawScale(canvas);        // 绘制刻度
        // onDrawHeading(canvas);
    }

    /**
     * 绘制中部固定箭头
     * @param canvas
     */
    private void onDrawArrow(Canvas canvas){
        int verticalLineLength = canvas.getHeight() / 6;
        Path arrowPath = new Path();
        arrowPath.moveTo(canvas.getWidth() / 2, 0);
        arrowPath.lineTo(canvas.getWidth() / 2 - verticalLineLength, 0);
        arrowPath.lineTo(canvas.getWidth() / 2, verticalLineLength);
        arrowPath.lineTo(canvas.getWidth() / 2 + verticalLineLength, 0);
        canvas.drawPath(arrowPath, mArrowPaint);
    }

    /**
     * 绘制中部刻度尺
     * @param canvas
     */
    private void onDrawScale(Canvas canvas){
        /* 设置刻度间隔，每隔1度的宽度 */
        int scaleSpacing = canvas.getHeight() / 10;
        /* 长刻度的高度 */
        int longScaleHeight = canvas.getHeight() * 3 / 6;
        /* 短刻度的高度 */
        int shortScaleHeight = canvas.getHeight() * 2 / 6;
        Path scalePath = new Path();
        /* 绘制左侧刻度 */
        for(int leftIndex = mCurrentHeading - ANGLE_RANGE; leftIndex <= mCurrentHeading + ANGLE_RANGE; leftIndex++){
            int offsetAngle = mCurrentHeading - leftIndex;                  // 计算偏移的角度
            int offsetPixel = scaleSpacing * offsetAngle;                   // 计算偏移的像素
            scalePath.moveTo(canvas.getWidth() / 2 - offsetPixel, canvas.getHeight() / 6);
            if(leftIndex % 5 == 0){
                if(leftIndex % 10 == 0){
                    scalePath.rLineTo(0, longScaleHeight);
                    /* 绘制刻度底部角度值 */
                    int realHeading = (int) calibrateAngle(leftIndex);
                    String realHeadingStr;
                    switch (realHeading){
                        case 0:
                            realHeadingStr = "北";
                            break;
                        case 90:
                            realHeadingStr = "东";
                            break;
                        case -90:
                            realHeadingStr = "西";
                            break;
                        case 180:
                        case -180:
                            realHeadingStr = "南";
                            break;
                        default:
                            realHeadingStr = realHeading + "°";
                            break;
                    }
                    canvas.drawText(realHeadingStr, canvas.getWidth() / 2 - offsetPixel, canvas.getHeight() / 2 + longScaleHeight, mScaleTextPaint);
                }else{
                    scalePath.rLineTo(0, shortScaleHeight);
                }
            }
        }
        canvas.drawPath(scalePath, mScalePaint);
    }

    /**
     * 绘制底部数值显示
     * @param canvas
     */
    private void onDrawHeading(Canvas canvas){
        Path headingTextPath = new Path();
        headingTextPath.moveTo(0, canvas.getHeight());
        headingTextPath.lineTo(canvas.getWidth(), canvas.getHeight());
        canvas.drawTextOnPath(mCurrentHeading + "°", headingTextPath, 0, -5, mScaleTextPaint);
    }

    /**
     * 校准角度值，将角度校准为(-180, 180]范围
     * @param angle
     * @return
     */
    private float calibrateAngle(float angle){
        if(angle > 180){
            angle = angle - 360;
        }else if(angle <= -180){
            angle = angle + 360;
        }
        return angle;
    }

}








