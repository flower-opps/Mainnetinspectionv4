package com.jizhenkeji.mainnetinspection.mission.followwirev2;

import androidx.annotation.Nullable;

import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.controller.virtualstickcontroller.VirtualStickController;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;
import com.jizhenkeji.mainnetinspection.radar.RadarManager;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;

/**
 * 公共必要参数：
 * {@link #setRadarManager(RadarManager)}          - 设置雷达适配器
 * {@link #setHeading(float)}                      - 设置无人机机头方向
 * {@link #setFlightDirection(FlightDirection)}    - 设置飞行方向
 * 公共可选参数：
 * {@link #setInspectionVelocity(float)}           - 设置仿线飞行速度
 * {@link #setVerticalDistanceToLine(float)}       - 设置垂直离线距离
 * {@link #setHorizontalDistanceToLine(float)}     - 设置水平离线距离
 * 内部参数配置：
 * {@link #setInspectionControlEnabled(boolean)}   - 设置巡检控制使能
 * {@link #setVerticalControlEnabled(boolean)}     - 设置垂直控制使能
 * {@link #setHorizontalControlEnabled(boolean)}   - 设置水平控制使能
 */
public class FollowWireV2Mission {

    /**
     * 定义系统的控制周期
     */
    private final long SYSTEM_CONTROL_CYCLE = 100;

    /* 默认仿线飞行速度 */
    private final float DEFAULT_INSPECTION_VELOCITY = 1f;

    /* 默认垂直方向离导线距离 */
    private final float DEFAULT_VERTICAL_DISTANCE_TO_LINE = -0.2f;

    /* 默认水平方向离导线距离 */
    private final float DEFAULT_HORIZONTAL_DISTANCE_TO_LINE = 3f;

    /* 仿线飞行速度 */
    private float mInspectionVelocity = DEFAULT_INSPECTION_VELOCITY;

    /* 垂直方向上离线距离 */
    private float mVerticalDistanceToLine = DEFAULT_VERTICAL_DISTANCE_TO_LINE;

    /* 垂直方向上的控制使能 */
    private boolean mVerticalControlEnabled = true;

    /* 水平方向上离线距离 */
    private float mHorizontalDistanceToLine = DEFAULT_HORIZONTAL_DISTANCE_TO_LINE;

    /* 水平方向上的控制使能 */
    private boolean mHorizontalControlEnabled = true;

    /* 雷达数据管理对象 */
    private RadarManager mRadarManager;

    /* 仿线飞行的机头方向 */
    private float mHeading;

    /* 仿线飞行方向 */
    private FlightDirection mFlightDirection;

    /* 巡检控制权 */
    private boolean mInspectionControlEnabled = true;

    public float getInspectionVelocity() {
        return mInspectionVelocity;
    }

    public float getVerticalDistanceToLine() {
        return mVerticalDistanceToLine;
    }

    public float getHorizontalDistanceToLine() {
        return mHorizontalDistanceToLine;
    }

    public float getHeading() {
        return mHeading;
    }

    @Nullable
    public RadarManager getRadarManager(){
        return mRadarManager;
    }

    public void setInspectionVelocity(float inspectionVelocity) {
        this.mInspectionVelocity = inspectionVelocity;
    }

    public void setVerticalDistanceToLine(float verticalDistanceToLine) {
        this.mVerticalDistanceToLine = -1.0f * verticalDistanceToLine;
    }

    public void setHorizontalDistanceToLine(float horizontalDistanceToLine) {
        this.mHorizontalDistanceToLine = horizontalDistanceToLine;
    }

    public void setRadarManager(RadarManager radarManager) {
        this.mRadarManager = radarManager;
    }

    public void setHeading(float heading) {
        this.mHeading = heading;
    }

    public FlightDirection getFlightDirection(){
        return mFlightDirection;
    }

    public void setFlightDirection(FlightDirection flightDirection){
        mFlightDirection = flightDirection;
    }

    private VirtualStickController mVirtualStickController;

    protected void onStart(){
        /* 设置虚拟摇杆控制坐标系 */
        mVirtualStickController = VirtualStickController.getInstance();
        mVirtualStickController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
    }

    protected void onResume(){
        /* 获取虚拟摇杆控制权 */
        mVirtualStickController.startControl(null);
    }

    protected void setInspectionControlEnabled(boolean enabled){
        mInspectionControlEnabled = enabled;
    }

    protected boolean getInspectionControlEnabled(){
        return mInspectionControlEnabled;
    }

    protected void onInspecting(){
        /* 校准机头方向 */
        if(!calibrateAircraftHeading()){
            return;
        }
        /* 校准相对导线位置 */
        calibrateAircraftLocation();
        /* 向终止塔方向巡检 */
        if(mInspectionControlEnabled){
            VirtualStickController.getInstance().setRollValue(mInspectionVelocity * mFlightDirection.getDirection());
        }else{
            VirtualStickController.getInstance().setRollValue(0);
        }
        /* 阻塞100ms */
        blockSystem(SYSTEM_CONTROL_CYCLE);
    }

    /**
     * 阻塞系统
     * @param time
     */
    protected void blockSystem(long time){
        try{
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 校准机头方向
     * @return true为校准完毕
     */
    private boolean calibrateAircraftHeading(){
        /* 校准无人机机头角度 */
        float angleError = Math.abs(mHeading - DJIFlightControlUtil.getAircraftHeading());
        if(angleError <= 2.5f || angleError >= 355f){
            /* 角度误差小于5度，转换到下一步 */
            return true;
        }
        onCalibrateAircraftHeading();
        VirtualStickController.getInstance().setYawValue(mHeading);
        calibrateVerticalLocation();        // 校准垂直方向上相对导线
        return false;
    }

    /**
     * 校准机头方向时调用
     */
    protected void onCalibrateAircraftHeading(){}

    /**
     * 校准无人机相对导线位置
     */
    private void calibrateAircraftLocation(){
        /* 校准无人机垂直方向上相对导线的位置 */
        calibrateVerticalLocation();
        /* 校准无人机水平方向上相对导线的位置 */
        calibrateHorizontalLocation();
    }

    protected boolean getVerticalControlEnabled(){
        return mVerticalControlEnabled;
    }

    protected void setVerticalControlEnabled(boolean verticalControlEnabled) {
        this.mVerticalControlEnabled = verticalControlEnabled;
    }

    protected boolean getHorizontalControlEnabled() {
        return mHorizontalControlEnabled;
    }

    protected void setHorizontalControlEnabled(boolean horizontalControlEnabled) {
        this.mHorizontalControlEnabled = horizontalControlEnabled;
    }

    /* 垂直方向上的比例参数 */
    private final float verticalKp = 1.5f;

    /* 垂直方向上的微分参数 */
    private final float verticalKd = 4f;

    /* 垂直方向上的上次误差 */
    private float verticalLastError;

    /**
     * 校准无人机垂直方向上相对导线的位置
     */
    private void calibrateVerticalLocation(){
        if(!mVerticalControlEnabled){
            VirtualStickController.getInstance().setVerticalThrottleValue(0);
            return;
        }
        float actualVerticalDistanceToLine = mRadarManager.getLineY();                 // 获取飞机实际离线距离
        float verticalError = getVerticalDistanceToLine() - actualVerticalDistanceToLine;   // 计算误差
        if(actualVerticalDistanceToLine == 0f || Math.abs(verticalError) < 0.1){
            // 雷达扫描不到导线或者误差较小则停止校准
            mVirtualStickController.setVerticalThrottleValue(0);
            return;
        }
        float verticalOutSpeed = verticalKp * verticalError + verticalKd * (verticalError - verticalLastError);
        verticalLastError = verticalError;
        mVirtualStickController.setVerticalThrottleValue(verticalOutSpeed);           // 输出控制
    }

    /* 水平方向上的比例参数 */
    private final float horizontalKp = -0.5f;

    /**
     * 校准无人机水平方向上相对导线的位置
     */
    private void calibrateHorizontalLocation(){
        if(!mHorizontalControlEnabled){
            VirtualStickController.getInstance().setPitchValue(0);
            return;
        }
        float actualHorizontalDistanceToLine = mRadarManager.getLineX();                     // 获取飞机实际离线距离
        float horizontalError = getHorizontalDistanceToLine() - actualHorizontalDistanceToLine;   // 计算误差
        if(actualHorizontalDistanceToLine == 0f || Math.abs(horizontalError) < 0.1){
            // 雷达扫描不到导线或者误差较小则停止校准
            mVirtualStickController.setPitchValue(0);
            return;
        }
        float horizontalOutSpeed = horizontalKp * horizontalError;
        /* 约束水平方向上的控制速度 */
        if(horizontalOutSpeed > 1f){
            horizontalOutSpeed = 1f;
        }else if(horizontalOutSpeed < -1f){
            horizontalOutSpeed = -1f;
        }
        mVirtualStickController.setPitchValue(horizontalOutSpeed);
    }

    protected void onPause(){
        /* 是否虚拟摇杆控制权 */
        mVirtualStickController.keepHover();
        mVirtualStickController.stopControl(null);
    }

    protected void onStop(){
        mVirtualStickController = null;
    }

}
