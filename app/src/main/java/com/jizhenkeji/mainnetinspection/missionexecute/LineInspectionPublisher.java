package com.jizhenkeji.mainnetinspection.missionexecute;

import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.controller.virtualstickcontroller.VirtualStickController;
import com.jizhenkeji.mainnetinspection.mission.followwire.SubscribeEvent;
import com.jizhenkeji.mainnetinspection.radar.RadarAdapter;
import com.jizhenkeji.mainnetinspection.radar.RadarManager;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;


public class LineInspectionPublisher extends InspectionParametersPublisher {

    /**
     * 仿线巡检内部状态
     */
    private LineInspectionState mState;

    /**
     * 虚拟摇杆控制对象
     */
    private VirtualStickController mVirtualStickController;

    /**
     * 雷达设备管理器
     */
    private RadarManager mRadarManager;

    /**
     * 无人机扫描雷达的起始角度
     */
    private float mStartScanHeading;

    /**
     * 无人机扫描雷达的终止角度
     */
    private float mEndScanHeading;

    /**
     * 用于存储拟合雷达扫描距离的容器
     */
    private WeightedObservedPoints mWeightedObservedPoints;

    private List<Float> mFitHeadingPoints;

    /**
     * 巡检过程中的机头偏航方向
     */
    private float mInspectionHeading;

    /**
     * 加载导线巡检任务，获取虚拟摇杆控制对象，获取雷达管理对象
     * @return
     */
    @Override
    protected JZIError onLoad() {
        mVirtualStickController = VirtualStickController.getInstance();
        mRadarManager = RadarAdapter.getRadarManager();
        return super.onLoad();
    }

    /**
     * 设置虚拟摇杆控制坐标系为无人机自身坐标系，发布巡检任务开始事件
     */
    @Override
    protected void onStart() {
        super.onStart();
        mVirtualStickController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        publishEvent(Event.ON_INSPECTING_START);
    }

    /**
     * 恢复巡检任务
     * 1、获取虚拟摇杆控制权
     * 2、如果没有杆塔数据，则为快速模式、否则为台账模式
     * 3、发布巡检任务恢复事件
     */
    @Override
    protected void onResume() {
        super.onResume();
        setFlightDirection(FlightDirection.UNKNOWN);
        mVirtualStickController.startControl(null);
        if(getStartTowerLocation() == null || getEndTowerLocation() == null){
            mStartScanHeading = getAircraftHeading() - getRadarScanRange() / 2;
            mEndScanHeading = getAircraftHeading() + getRadarScanRange() / 2;
            mWeightedObservedPoints = new WeightedObservedPoints();
            mFitHeadingPoints = new ArrayList<>();
            mState = LineInspectionState.STATE_INIT_RADAR_SCAN_ANGLE;
        }else{
            calculateHeadingWithTowerLocation(getAircraftHeading());
            mState = LineInspectionState.STATE_CALIBRATE_HEADING;
        }
        publishEvent(Event.ON_INSPECTING_RESUME);
    }

    @Override
    protected void onRunning() {
        super.onRunning();
        switch (mState){
            case STATE_INIT_RADAR_SCAN_ANGLE:
                /* 初始化雷达扫线初始角度 */
                onInitScanLineAngle(calibrateHeading(mStartScanHeading));
                break;
            case STATE_RADAR_SCANNING:
                /* 雷达扫线中 */
                onScanningLine();
                break;
            case STATE_CALIBRATE_HEADING:
                /* 校准机头方向 */
                onCalibrateHeading();
                break;
            case STATE_CALIBRATE_AIRCRAFT_LOCATION:
                /* 校准无人机位置信息 */
                onCalibrateAircraftLocation();
                break;
            case STATE_WAIT_CHOOSE_DIRECTION:
                /* 等待用户选择巡检飞行方向 */
                onWaitChooseDirection();
                break;
            case STATE_INSPECTING:
                /* 雷达扫线中 */
                onInspecting();
                break;
        }
    }

    /**
     * 快速模式，处于初始化雷达扫线角度的时候调用
     * @param startScanHeading 扫描导线的起始角度，范围[-180, 180]
     */
    private void onInitScanLineAngle(float startScanHeading){
        mVirtualStickController.setYawValue(startScanHeading);
        /* 计算误差值 */
        float angleError = Math.abs(calibrateHeading(startScanHeading) - getAircraftHeading());
        if(angleError <= 2.5f || angleError >= 355f){
            /* 角度误差小于5度，开始雷达扫描 */
            mWeightedObservedPoints = new WeightedObservedPoints();
            mState = LineInspectionState.STATE_RADAR_SCANNING;
        }
    }

    /**
     * 快速模式独有，处于雷达扫描导线的时候调用
     */
    protected void onScanningLine(){
        if(mStartScanHeading < mEndScanHeading){
            /* 正在扫描角度 */
            mVirtualStickController.setYawValue(calibrateHeading(mStartScanHeading));
            mStartScanHeading++;
            /* 记录拟合数据 */
            if(mRadarManager.getLineX() != 0f){
                float currentHeading = getAircraftHeading();
                float xDistanceToline = mRadarManager.getLineX();
                mWeightedObservedPoints.add(mFitHeadingPoints.size(), xDistanceToline);
                mFitHeadingPoints.add(currentHeading);
            }
        }else{
            /* 计算离线最小距离的角度 */
            List<WeightedObservedPoint> weightedObservedPoints = mWeightedObservedPoints.toList();
            if(weightedObservedPoints == null || weightedObservedPoints.size() <= 3){
                mStartScanHeading = mEndScanHeading - getRadarScanRange();
                mState = LineInspectionState.STATE_INIT_RADAR_SCAN_ANGLE;
                return;
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);
            double[] fitResult = fitter.fit(weightedObservedPoints);
            double fitA = fitResult[2];
            double fitB = fitResult[1];
            double fitC = fitResult[0];
            int fitIndex = (int) (- fitB / (2 * fitA));
            mInspectionHeading = mFitHeadingPoints.get(fitIndex);
            mState = LineInspectionState.STATE_CALIBRATE_HEADING;
        }
        /* 校准垂直方向上离线距离 */
        calibrateVerticalLocation();
    }

    @SubscribeEvent(event = Event.CONFIGURE_LEFT_DIRECTION)
    public void onChooseLeftDirection(){
        setFlightDirection(FlightDirection.LEFT);
    }

    @SubscribeEvent(event = Event.CONFIGURE_RIGHT_DIRECTION)
    public void onChooseRightDirection(){
        setFlightDirection(FlightDirection.RIGHT);
    }

    /**
     * 模式共用，等待用户选择巡检方向
     */
    protected void onWaitChooseDirection(){
        VirtualStickController.getInstance().setYawValue(mInspectionHeading);
        if(getFlightDirection() != FlightDirection.UNKNOWN){
            /* 用户完成巡检方向的选择，开始执行导线巡检任务 */
            mState = LineInspectionState.STATE_INSPECTING;
        }
        calibrateAircraftLocation();        // 校准垂直、水平方向上相对导线
    }

    /**
     * 模式共用，校准机头方向时调用
     */
    protected void onCalibrateHeading(){
        /* 校准无人机机头角度 */
        float angleError = Math.abs(mInspectionHeading - getAircraftHeading());
        if(angleError <= 2.5f || angleError >= 357.5f){
            /* 角度误差小于5度，转换到下一步 */
            mState = LineInspectionState.STATE_CALIBRATE_AIRCRAFT_LOCATION;
            return;
        }
        mVirtualStickController.setYawValue(mInspectionHeading);
    }

    /**
     * 模式共用，校准无人机相对导线位置
     */
    protected void onCalibrateAircraftLocation(){
        /* 当误差处于可接受范围才进入下一步 */
        //if(Math.abs(getVerticalDistanceToLine() - mRadarManager.getLineY()) < 0.1
        //        && Math.abs(getHorizontalDistanceToLine() - mRadarManager.getLineX()) < 0.2){
        if(true){
            mState = LineInspectionState.STATE_WAIT_CHOOSE_DIRECTION;
            publishEvent(Event.ON_WAIT_INSPECTION_DIRECTION);
            return;
        }
        /* 校准飞机/相对导线位置 */
        calibrateAircraftLocation();
    }

    /**
     * 巡检方向的飞行控制权
     */
    private boolean mInspectionControl = true;

    /**
     * 设置巡检方向上的飞行控制权
     * @param isEnable
     */
    protected void setInspectionControlEnable(boolean isEnable){
        this.mInspectionControl = isEnable;
    }

    @SubscribeEvent(event = Event.PAUSE_INSPECTION_CONTROL)
    public void pauseInspectionControl(){
        setInspectionControlEnable(false);
    }

    @SubscribeEvent(event = Event.RESUME_INSPECTION_CONTROL)
    public void resumeInspectionControl(){
        setInspectionControlEnable(true);
    }

    protected void onInspecting(){
        /* 校准相对导线位置 */
        calibrateAircraftLocation();
        /* 判断是否到达终点 */
        if (getStartLocation() != null && getEndLocation() != null) {
            Location aircraftLocation = getAircraftLocation();
            float distanceToEndLocation = GPSUtil.calculateLineDistance(
                    aircraftLocation.latitude,
                    aircraftLocation.longitude,
                    getEndLocation().latitude,
                    getEndLocation().longitude
            );
            if(distanceToEndLocation < getSafeStopDistance()) {
                finish();
            } else {
                double x0 = getStartLocation().latitude - getEndLocation().latitude;
                double y0 = getStartLocation().longitude - getEndLocation().longitude;
                double x1 = aircraftLocation.latitude - getEndLocation().latitude;
                double y1 = aircraftLocation.longitude - getEndLocation().longitude;
                double dot = x0 * x1 + y0 * y1;
                if (dot < 0 && (distanceToEndLocation > 0.1)) {
                    finish();
                }
            }
        } else if (getEndLocation() != null) {
            Location aircraftLocation = getAircraftLocation();
            float distanceToEndLocation = GPSUtil.calculateLineDistance(
                    aircraftLocation.latitude,
                    aircraftLocation.longitude,
                    getEndLocation().latitude,
                    getEndLocation().longitude
            );
            if(distanceToEndLocation < getSafeStopDistance()) {
                finish();
            }
        }
        /* 向终止塔方向巡检 */
        if(mInspectionControl){
            VirtualStickController.getInstance().setRollValue(getInspectionSpeed() * getFlightDirection().getDirection());
        }else{
            VirtualStickController.getInstance().setRollValue(0);
        }
        /* 发布导线巡检事务 */
        publishEvent(Event.ON_INSPECTING);
    }

    /**
     * 校准无人机相对导线位置
     */
    private void calibrateAircraftLocation(){
        /* 校准无人机垂直方向上相对导线的位置 */
        calibrateVerticalLocation();
        /* 校准无人机水平方向上相对导线的位置 */
        calibrateHorizontalLocation();
    }

    /**
     * 垂直方向上的比例参数
     */
    private final float verticalKp = 1.5f;

    /**
     * 垂直方向上的微分参数
     */
    private final float verticalKd = 1f;

    /**
     * 垂直方向上的控制权
     */
    private boolean mVerticalControl = true;

    /**
     * 垂直方向上的上次误差
     */
    private float verticalLastError;

    /**
     * 设置垂直方向上的控制使能
     * @param isEnable
     */
    protected void setVerticalControlEnable(boolean isEnable){
        this.mVerticalControl = isEnable;
    }

    /**
     * 校准无人机垂直方向上相对导线的位置
     */
    private void calibrateVerticalLocation(){
        if(!mVerticalControl){
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
        mVirtualStickController.getInstance().setVerticalThrottleValue(verticalOutSpeed);           // 输出控制
    }

    /**
     * 水平方向上的比例参数
     */
    private final float horizontalKp = -0.5f;

    /**
     * 水平方向上的控制权
     */
    private boolean mHorizontalControl = true;

    /**
     * 设置水平方向上的控制使能
     * @param isEnable
     */
    protected void setHorizontalControlEnable(boolean isEnable){
        this.mHorizontalControl = isEnable;
    }

    /**
     * 校准无人机水平方向上相对导线的位置
     */
    private void calibrateHorizontalLocation(){
        if(!mHorizontalControl){
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

    /**
     * 使用杆塔数据校准机头方向
     * @param aircraftYaw 当前飞机机头方向
     */
    private void calculateHeadingWithTowerLocation(float aircraftYaw){
        if(Float.isNaN(aircraftYaw)){
            return;
        }
        /* 计算导线角度 */
        float lineAngle = (float) GPSUtil.getAngleBetweenGpsCoordinate(
                getStartTowerLocation().latitude, getStartTowerLocation().longitude, getEndTowerLocation().latitude, getEndTowerLocation().longitude);
        /* 计算偏置之后的机头角度 */
        float offsetlineAngle = -lineAngle;
        float offsetHeadingAngle = calibrateHeading(aircraftYaw + offsetlineAngle);
        /* 计算实际机头巡检角度 */
        if(offsetHeadingAngle > -175 && offsetHeadingAngle < -5){
            mInspectionHeading = calibrateHeading(lineAngle - 90);
        }else if(offsetHeadingAngle > 5 && offsetHeadingAngle < 175){
            mInspectionHeading = calibrateHeading(lineAngle + 90);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        publishEvent(Event.ON_INSPECTING_PAUSE);
        /* 停止巡检动作 */
        mVirtualStickController.keepHover();
        mVirtualStickController.stopControl(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        publishEvent(Event.ON_INSPECTING_STOP);
        //mVirtualStickController.keepHover();
        //mVirtualStickController.stopControl(null);
    }

    public static class Event {

        public static final String ON_INSPECTING_START = "LineInspectionPublisher.ON_INSPECTING_START";

        public static final String ON_INSPECTING_RESUME = "LineInspectionPublisher.ON_INSPECTING_RESUME";

        public static final String ON_WAIT_INSPECTION_DIRECTION = "LineInspectionPublisher.ON_WAIT_INSPECTION_DIRECTION";

        public static final String ON_INSPECTING = "LineInspectionPublisher.ON_INSPECTING";

        public static final String ON_INSPECTING_PAUSE = "LineInspectionPublisher.ON_INSPECTING_PAUSE";

        public static final String ON_INSPECTING_STOP = "LineInspectionPublisher.ON_INSPECTING_STOP";

        public static final String RESUME_INSPECTION_CONTROL = "LineInspectionPublisher.RESUME_INSPECTION_CONTROL";

        public static final String PAUSE_INSPECTION_CONTROL = "LineInspectionPublisher.PAUSE_INSPECTION_CONTROL";

        public static final String CONFIGURE_LEFT_DIRECTION = "LineInspectionPublisher.CONFIGURE_LEFT_DIRECTION";

        public static final String CONFIGURE_RIGHT_DIRECTION = "LineInspectionPublisher.CONFIGURE_RIGHT_DIRECTION";

    }

    private enum LineInspectionState{

        /**
         * 快速模式：初始化雷达扫描角度
         */
        STATE_INIT_RADAR_SCAN_ANGLE,

        /**
         * 快速模式：雷达正在对线扫描
         */
        STATE_RADAR_SCANNING,

        /**
         * 快速模式：等待用户选择巡检方向
         * 导线模式：等待用户选择巡检方向
         */
        STATE_WAIT_CHOOSE_DIRECTION,

        /**
         * 校准机头方向
         */
        STATE_CALIBRATE_HEADING,

        /**
         * 校准飞机相对导线位置
         */
        STATE_CALIBRATE_AIRCRAFT_LOCATION,

        /**
         * 向巡检方向进行巡检
         */
        STATE_INSPECTING;

    }

}
