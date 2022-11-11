package com.jizhenkeji.mainnetinspection.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.Nullable;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.controller.gimbalcontroller.GimbalController;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import java.text.DecimalFormat;

import dji.common.flightcontroller.LocationCoordinate3D;

/**
 * 巡线任务信息展示
 */
public class TowerLabelView extends View {

    private final float BOX_PADDING = GlobalUtils.dpToPx(5);

    private final DecimalFormat df = new DecimalFormat("#.#");

    /* 默认警告距离 */
    private final float DEFAULT_ALERT_DISTANCE = 5f;

    /* 默认标签显示距离限制 */
    private final float DEFAULT_LABEL_DISPLAY_DISTANCE = 15f;

    /* 存储巡检任务及杆塔数据 */
    private MissionWithTowers mMissionWithTowers;

    /* 水平方向上每度的像素值 */
    private float mHorizontalPixelPreAngle;

    /* 垂直方向上每度的像素值 */
    private float mVerticalPixelPreAngle;

    /* 杆塔标签的文本画笔 */
    private Paint mTextPaint;

    /* 杆塔标签的背景画笔 */
    private Paint mBackGroundPaint;

    public TowerLabelView(Context context) {
        super(context);
        initView();
    }

    public TowerLabelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView(){
        /* 初始化标签的背景画笔对象 */
        mBackGroundPaint = new Paint();
        mBackGroundPaint.setColor(GlobalUtils.getColor(R.color.lucencyWhite));
        mBackGroundPaint.setStyle(Paint.Style.FILL);
        /* 初始化标签的文本画笔对象 */
        mTextPaint = new Paint();
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMissionWithTowers(MissionWithTowers missionWithTowers){
        mMissionWithTowers = missionWithTowers;
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float dfov = CameraController.getInstance().getDFOV();                              // 获取视角角度
        float hypotenuse = (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));     // 获取斜边长度
        /* 计算水平垂直方向上每度代表的像素值 */
        mHorizontalPixelPreAngle = width / (dfov * width / hypotenuse);                     // 计算水平方向上每度的像素值
        mVerticalPixelPreAngle = height / (dfov * height / hypotenuse);                     // 垂直方向上每度的像素值
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        /* 判断是否绘制杆塔标签 */
        if(mMissionWithTowers == null || mMissionWithTowers.towerEntities.isEmpty()){
            return;
        }
        float gimbalPitch = GimbalController.getInstance().getPitchAngle();                     // 云台俯仰角度
        float aircraftHeading = DJIFlightControlUtil.getAircraftHeading();                      // 无人机偏航角
        LocationCoordinate3D currentLocation = DJIFlightControlUtil.getAircraftLocation();      // 无人机坐标
        if(currentLocation == null){
            return;
        }
        /* 迭代杆塔对象并绘制标签 */
        for(TowerEntity towerEntity : mMissionWithTowers.towerEntities){
            float distanceToTower = GPSUtil.calculateLineDistance(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    towerEntity.location.latitude,
                    towerEntity.location.longitude
            );
            if(distanceToTower >= DEFAULT_LABEL_DISPLAY_DISTANCE){
                /* 距离不满足要求，跳过 */
                continue;
            }
            if(distanceToTower <= DEFAULT_ALERT_DISTANCE){
                mTextPaint.setColor(GlobalUtils.getColor(R.color.rallyOrange));
            }else{
                mTextPaint.setColor(GlobalUtils.getColor(R.color.white));
            }
            /* 计算无人机对杆塔的偏航角度 */
            float aircraftToTowerYawAngle = (float) GPSUtil.getAngleBetweenGpsCoordinate(      // 无人机对杆塔的角度
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    towerEntity.location.latitude,
                    towerEntity.location.longitude
            );
            /* 计算水平方向上的偏移角度 */
            float horizontalOffsetAngle = GPSUtil.calibrateHeading(aircraftToTowerYawAngle - aircraftHeading);
            /* 计算无人机对塔的俯仰角度，默认杆塔标签与无人机处于同一水平 */
            float aircraftToTowerPitchAngle = 0f;
            /* 计算垂直方向上的偏移角度 */
            float verticalOffsetAngle = aircraftToTowerPitchAngle + gimbalPitch;
            /* 计算水平和垂直方向上的偏移像素 */
            float horizontalOffsetPixel = horizontalOffsetAngle * mHorizontalPixelPreAngle;
            float verticalOffsetPixel = verticalOffsetAngle * mVerticalPixelPreAngle;
            /* 绘制杆塔标签 */
            float labelTextSize = getCurrentLabelTextSize(distanceToTower);
            mTextPaint.setTextSize(labelTextSize);
            String towerName = mMissionWithTowers.missionEntity.name + "#" + towerEntity.towerNum + " " + mMissionWithTowers.missionEntity.voltageLevel;
            canvas.drawText(towerName, horizontalOffsetPixel, verticalOffsetPixel, mTextPaint);
            canvas.drawText(df.format(distanceToTower) + "米", horizontalOffsetPixel, verticalOffsetPixel + labelTextSize, mTextPaint);
            float boxWidth = towerName.length() * labelTextSize;
            float boxXOffset = boxWidth / 2 + BOX_PADDING;
            float boxYOffset = labelTextSize + BOX_PADDING;
            canvas.drawRect(horizontalOffsetPixel - boxXOffset,
                    verticalOffsetPixel - boxYOffset,
                    horizontalOffsetPixel + boxXOffset,
                    verticalOffsetPixel + boxYOffset, mBackGroundPaint);
        }
        postInvalidateDelayed(50);
    }

    /**
     * 获取当前距离下标签的文本大小
     * @param distance 离杆塔距离
     * @return 文本大小，单位px
     */
    private float getCurrentLabelTextSize(float distance){
        return (DEFAULT_LABEL_DISPLAY_DISTANCE - distance) * GlobalUtils.dpToPx(5);
    }

}
