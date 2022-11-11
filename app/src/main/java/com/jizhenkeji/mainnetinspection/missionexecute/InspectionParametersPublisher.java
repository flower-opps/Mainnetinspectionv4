package com.jizhenkeji.mainnetinspection.missionexecute;

import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.mission.followwire.BasePublisher;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;

public abstract class InspectionParametersPublisher extends BasePublisher {

    /**
     * 定义系统的控制周期
     */
    private final long SYSTEM_CONTROL_CYCLE = 100;

    /**
     * 定义快速模式下，雷达扫描导线的范围
     */
    private final float RADAR_SCAN_RANGE = 120f;

    /**
     * 巡检任务安全停止距离
     */
    private final int SAFE_STOP_DISTANCE = 15;

    /**
     * 巡检的起始杆塔，特指小号塔
     */
    private Location mStartTowerLocation;

    /**
     * 巡检的终止杆塔，特指大号塔
     */
    private Location mEndTowerLocation;

    /**
     * 巡检的终止坐标点
     */
    private Location mEndLocation;

    /**
     * 巡检的起始坐标点
     */
    private Location mStartLocation;

    /**
     * 快速模式下的飞行方向
     */
    private FlightDirection mFlightDirection = FlightDirection.UNKNOWN;

    /**
     * 无人机巡检飞行速度
     */
    private float mInspectionSpeed = 1f;

    /**
     * 无人机垂直离线距离
     */
    private float mVerticalDistanceToLine = 0.2f;

    /**
     * 无人机水平离线距离
     */
    private float mHorizontalDistanceToLine = 3f;

    /**
     * 雷达扫描范围
     * @return
     */
    public float getRadarScanRange(){
        return RADAR_SCAN_RANGE;
    }

    /**
     * 获取杆塔巡检安全停止距离
     * @return
     */
    public int getSafeStopDistance(){
        return SAFE_STOP_DISTANCE;
    }

    /**
     * 设置无人机起始杆塔位置
     * @param startTowerLocation
     */
    public void setStartTowerLocation(Location startTowerLocation){
        mStartTowerLocation = startTowerLocation;
    }

    /**
     * 获取无人机起始杆塔位置
     * @return
     */
    public Location getStartTowerLocation(){
        return mStartTowerLocation;
    }

    /**
     * 设置无人机终止杆塔位置
     * @param endTowerLocation
     */
    public void setEndTowerLocation(Location endTowerLocation){
        mEndTowerLocation = endTowerLocation;
    }

    /**
     * 获取无人机终止杆塔位置
     * @return
     */
    public Location getEndTowerLocation(){
        return mEndTowerLocation;
    }

    /**
     * 获取杆塔巡检停止坐标
     * @return
     */
    public Location getEndLocation() {
        return mEndLocation;
    }

    /**
     * 获取杆塔巡检起始坐标
     * @return
     */
    public Location getStartLocation() {
        return mStartLocation;
    }

    /**
     * 设置无人机飞行方向
     * @param flightDirection
     */
    protected void setFlightDirection(FlightDirection flightDirection){
        mFlightDirection = flightDirection;
        // 当用户配置完成巡检方向之后，计算巡检任务的终止点位置，终止点的选取存在以下三种情况：
        // 1、大小号塔都在无人机的巡检方向上，则选取离无人机最远的塔为终止点
        // 2、只有单座塔在无人机的巡检方向上，则选取此塔为终止点
        // 3、没有任何一座塔处于无人机的巡检方向上，则直接终止退出任务
        if(mStartTowerLocation != null && mEndTowerLocation != null && flightDirection != FlightDirection.UNKNOWN) {
            float currentAircraftHeading = getAircraftHeading();
            boolean isStartTowerLocationWithinDirection =
                    isLocationWithinDirection(mStartTowerLocation, currentAircraftHeading, flightDirection);
            boolean isEndTowerLocationWithinDirection =
                    isLocationWithinDirection(mEndTowerLocation, currentAircraftHeading, flightDirection);
            if(isStartTowerLocationWithinDirection && isEndTowerLocationWithinDirection) {
                /* 两座塔都位于一侧 */
                Location aircraftLocation = getAircraftLocation();
                float distanceToStartTower = GPSUtil.calculateLineDistance(
                        aircraftLocation.latitude,
                        aircraftLocation.longitude,
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude
                );
                float distanceToEndTower = GPSUtil.calculateLineDistance(
                        aircraftLocation.latitude,
                        aircraftLocation.longitude,
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude
                );
                /* 终止点为距离最远的那座塔 */
                mEndLocation = distanceToEndTower >= distanceToStartTower ? mEndTowerLocation : mStartTowerLocation;
                mStartLocation = distanceToEndTower < distanceToStartTower ? mEndTowerLocation : mStartTowerLocation;
            }else if(isStartTowerLocationWithinDirection) {
                /* 只有起始塔位于一侧，终止点为起始塔 */
                mEndLocation = mStartTowerLocation;
                mStartLocation = mEndTowerLocation;
            }else if(isEndTowerLocationWithinDirection) {
                /* 只有终止塔位于一侧，终止点为终止塔 */
                mEndLocation = mEndTowerLocation;
                mStartLocation = mStartTowerLocation;
            }else {
                /* 没有任何塔在一侧，直接退出巡检任务 */
                finish();
            }
        }
    }

    /**
     * 判断指定的目标坐标是否在无人机的某一侧
     * @param targetLocation 指定坐标位置
     * @param heading 当前无人机机头方向
     * @param flightDirection 指向的范围
     * @return
     */
    private boolean isLocationWithinDirection(Location targetLocation, float heading, FlightDirection flightDirection) {
        Location aircraftLocation = getAircraftLocation();
        float targetHeading = (float) GPSUtil.getAngleBetweenGpsCoordinate(
                aircraftLocation.latitude,
                aircraftLocation.longitude,
                targetLocation.latitude,
                targetLocation.longitude);
        float targetHeadingOffset = calibrateHeading(targetHeading - heading);
        switch (flightDirection) {
            case LEFT:
                if(targetHeadingOffset > -180f && targetHeadingOffset < 0f) {
                    return true;
                }
                break;
            case RIGHT:
                if(targetHeadingOffset > 0f && targetHeadingOffset < 180f) {
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * 获取无人机飞行方向
     * @return
     */
    public FlightDirection getFlightDirection(){
        return mFlightDirection;
    }

    /**
     * 设置无人机巡检飞行速度
     * @param speed
     */
    public void setInspectionSpeed(float speed){
        mInspectionSpeed = speed;
    }

    /**
     * 获取无人机巡检飞行速度
     * @return
     */
    public float getInspectionSpeed(){
        return mInspectionSpeed;
    }

    /**
     * 设置无人机垂直离线距离
     * @param verticalDistanceToLine
     */
    public void setVerticalDistanceToLine(float verticalDistanceToLine){
        mVerticalDistanceToLine = verticalDistanceToLine;
    }

    /**
     * 获取无人机离线距离
     * @return
     */
    public float getVerticalDistanceToLine(){
        return mVerticalDistanceToLine;
    }

    /**
     * 设置无人机水平离线距离
     * @param horizontalDistanceToLine
     */
    public void setHorizontalDistanceToLine(float horizontalDistanceToLine){
        mHorizontalDistanceToLine = horizontalDistanceToLine;
    }

    /**
     * 获取无人机水平离线距离
     * @return
     */
    public float getHorizontalDistanceToLine(){
        return mHorizontalDistanceToLine;
    }

    /**
     * 校准无人机偏航角度值，将角度校准为(-180, 180]范围
     * @param angle
     * @return
     */
    protected float calibrateHeading(float angle){
        if(angle > 180){
            angle = angle - 360;
        }else if(angle <= -180){
            angle = angle + 360;
        }
        return angle;
    }

    @Override
    protected void onRunning() {
        super.onRunning();
        try{
            Thread.sleep(SYSTEM_CONTROL_CYCLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
