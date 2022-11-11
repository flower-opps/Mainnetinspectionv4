package com.jizhenkeji.mainnetinspection.controller.virtualstickcontroller;

import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * 虚拟摇杆控制组件
 */
public class VirtualStickController extends ComponentController {

    private final String TAG = "VirtualStickController";

    public static VirtualStickController getInstance(){
        return Controller.INSTANCE;
    }

    private static class Controller {
        private static final VirtualStickController INSTANCE = new VirtualStickController();
    }

    private VirtualStickController(){}

    private FlightController mFlightController;

    private VirtualStickControllerState mState = VirtualStickControllerState.NOT_CONTROLLED;

    private VirtualStickControlThread mControlThread;

    /**
     * 俯仰方向上的值，速度或者角度
     */
    private float mPitchValue;

    /**
     * 滚动方向上的值，速度或者角度
     */
    private float mRollValue;

    /**
     * 机身偏航方向上的值，角度或者速度
     */
    private float mYawValue;

    /**
     * 垂直方向上的值，高度或者速度
     */
    private float mVerticalThrottleValue;

    /**
     * 控制命令发送的频率, 为了保持能正常控制，发送频率需要保持在5Hz-25hz之间
     */
    private int mControlFrequency = 15;

    @Override
    public JZIError init() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft == null){
            return JZIError.NO_CONNECTED_TO_PRODUCT;
        }
        mFlightController = aircraft.getFlightController();
        /* 初始化控制模式 */
        mFlightController.setVirtualStickAdvancedModeEnabled(true);
        setVerticalControlMode(VerticalControlMode.VELOCITY);
        setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        setYawControlMode(YawControlMode.ANGLE);
        /* 获取当前控制状态 */
        FlightControllerKey controlStateKey = FlightControllerKey.create(FlightControllerKey.VIRTUAL_STICK_CONTROL_MODE_ENABLED);
        Boolean isControl = (Boolean) KeyManager.getInstance().getValue(controlStateKey);
        if(isControl != null && isControl){
            openControlThread();
            mState = VirtualStickControllerState.CONTROLLED;
        }
        return null;
    }

    /**
     * 获取虚拟摇杆控制权
     * @param callback
     */
    public synchronized void startControl(CommonCallback<JZIError> callback){
        if(mState == VirtualStickControllerState.CONTROLLED || mState == VirtualStickControllerState.PAUSE_CONTROL){
            if(callback != null) callback.onResult(VirtualStickControllerError.ALREADY_IN_CONTROL);
            return;
        }
        /* 判断是否处于飞行状态 */
        if(!mFlightController.getState().isFlying()){
            if(callback != null) callback.onResult(VirtualStickControllerError.NOT_IN_FLIGHT);
            return;
        }
        /* 获取虚拟摇杆控制权 */
        mFlightController.setVirtualStickModeEnabled(true, (DJIError djiError) -> {
            if(djiError != null){
                if(callback != null) callback.onResult(VirtualStickControllerError.UNKNOWN);
            }else{
                keepHover();
                openControlThread();
                mState = VirtualStickControllerState.CONTROLLED;
                if(callback != null) callback.onResult(null);
            }
        });
    }

    /**
     * 暂停虚拟摇杆控制，暂停状态并不释放虚拟摇杆控制权
     * @param callback
     */
    public synchronized void pauseControl(CommonCallback<JZIError> callback){
        if(mState != VirtualStickControllerState.CONTROLLED){
            if(callback != null) callback.onResult(VirtualStickControllerError.NOT_IN_CONTROL);
            return;
        }
        mControlThread.mStateControlLock.lock();
        try{
            if(!mControlThread.mPauseStateCondition.await(500, TimeUnit.MILLISECONDS)){
                if(callback != null) callback.onResult(VirtualStickControllerError.TIME_OUT);
            }else{
                if(callback != null) callback.onResult(null);
            }
        } catch (InterruptedException e) {
            if(callback != null) callback.onResult(null);
        }
        mControlThread.mStateControlLock.unlock();
    }

    /**
     * 恢复虚拟摇杆控制，无人机恢复上次暂停时的飞行状态
     * @param callback
     */
    public synchronized void resumeControl(CommonCallback<JZIError> callback){
        if(mState != VirtualStickControllerState.PAUSE_CONTROL){
            if(callback != null) callback.onResult(VirtualStickControllerError.NOT_IN_PAUSE_CONTROL);
            return;
        }
        mControlThread.mStateControlLock.lock();
        mControlThread.mPauseStateCondition.signalAll();
        mControlThread.mStateControlLock.unlock();
    }

    /**
     * 释放虚拟摇杆控制权
     * @param callback
     */
    public synchronized void stopControl(CommonCallback<JZIError> callback){
        keepHover();
        /* 放弃虚拟摇杆控制权 */
        mFlightController.setVirtualStickModeEnabled(false, (DJIError djiError) -> {
            if(djiError != null){
                if(callback != null) callback.onResult(VirtualStickControllerError.UNKNOWN);
            }else{
                if(mControlThread != null){
                    mControlThread.interrupt();
                }
                mState = VirtualStickControllerState.NOT_CONTROLLED;
                if(callback != null) callback.onResult(null);
            }
        });
    }

    /**
     * 悬停，控制无人机悬停，并不释放虚拟摇杆控制权
     */
    public void keepHover(){
        mPitchValue = 0f;
        mRollValue = 0f;
        if(mFlightController.getYawControlMode() == YawControlMode.ANGULAR_VELOCITY) {
            mYawValue = 0f;
        }else{
            mYawValue = mFlightController.getCompass().getHeading();
        }
        if(mFlightController.getVerticalControlMode() == VerticalControlMode.VELOCITY){
            mVerticalThrottleValue = 0;
        }else{
            mVerticalThrottleValue = mFlightController.getState().getAircraftLocation().getAltitude();
        }
    }

    /**
     * 获取当前虚拟控制组件内部状态
     * @return
     */
    public VirtualStickControllerState getState(){
        return mState;
    }

    /**
     * 设置俯仰方向的控制值
     * @param value 速度值或角度值
     */
    public void setPitchValue(float value){
        mPitchValue = value;
    }

    /**
     * 设置滚动方向的控制值
     * @param value 速度值或角度值
     */
    public void setRollValue(float value){
        mRollValue = value;
    }

    /**
     * 设置偏航方向的控制值
     * @param value 速度值或绝对角度
     */
    public void setYawValue(float value){
        mYawValue = value;
    }

    /**
     * 设置垂直方向上的控制值
     * @param value 速度值或绝对高度
     */
    public void setVerticalThrottleValue(float value){
        mVerticalThrottleValue = value;
    }

    /**
     * 设置控制信号发送频率
     * @param frequency 范围为5-25HZ
     */
    public void setControlFrequency(int frequency){
        if(frequency < 5){
            mControlFrequency = 5;
        }else if(frequency > 25){
            mControlFrequency = 25;
        }else{
            mControlFrequency = frequency;
        }
    }

    /**
     * 设置垂直方向上的控制模式
     * @param verticalControlMode
     */
    public void setVerticalControlMode(VerticalControlMode verticalControlMode){
        mFlightController.setVerticalControlMode(verticalControlMode);
    }

    /**
     * 设置俯仰与滚动方向的坐标系
     * @param coordinateSystem
     */
    public void setRollPitchCoordinateSystem(FlightCoordinateSystem coordinateSystem){
        mFlightController.setRollPitchCoordinateSystem(coordinateSystem);
    }

    /**
     * 设置俯仰与滚动方向的控制模式
     * @param rollPitchControlMode
     */
    public void setRollPitchControlMode(RollPitchControlMode rollPitchControlMode){
        mFlightController.setRollPitchControlMode(rollPitchControlMode);
    }

    /**
     * 设置偏航角控制模式
     * @param yawControlMode
     */
    public void setYawControlMode(YawControlMode yawControlMode){
        if(yawControlMode == YawControlMode.ANGLE){
            mYawValue = mFlightController.getCompass().getHeading();
        }
        mFlightController.setYawControlMode(yawControlMode);
    }

    /**
     * 开启控制线程
     */
    private void openControlThread(){
        if(mControlThread != null && mControlThread.isAlive()){
            mControlThread.interrupt();
        }
        mControlThread = new VirtualStickControlThread();
        mControlThread.start();
    }

    private class VirtualStickControlThread extends Thread {

        private ReentrantLock mStateControlLock = new ReentrantLock();

        private Condition mPauseStateCondition = mStateControlLock.newCondition();

        @Override
        public void run() {
            /* 按照一定的频率发送控制命令 */
            try{
                while(!Thread.interrupted()){
                    FlightControlData controlData = new FlightControlData(mRollValue, mPitchValue, mYawValue, mVerticalThrottleValue);
                    mFlightController.sendVirtualStickFlightControlData(controlData, null);
                    Thread.sleep(1000 / mControlFrequency);     // 根据发送频率计算间隔时间
                    /* 判断是否存在暂停信号 */
                    mStateControlLock.lock();
                    if(mStateControlLock.hasWaiters(mPauseStateCondition)){
                        mPauseStateCondition.signal();
                        mState = VirtualStickControllerState.PAUSE_CONTROL;
                        mPauseStateCondition.await();
                        mState = VirtualStickControllerState.CONTROLLED;
                    }
                    mStateControlLock.unlock();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "VirtualStickControlThread Interrupted");
            }
        }

    }

}
