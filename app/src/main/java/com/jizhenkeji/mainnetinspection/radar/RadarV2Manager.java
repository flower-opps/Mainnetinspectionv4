package com.jizhenkeji.mainnetinspection.radar;

import android.util.Log;

import dji.sdk.payload.Payload;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class RadarV2Manager implements RadarManager {

    private Payload mPayload;

    /**
     * 雷达数据最新获取时间戳
     */
    private volatile long mLastUpdateTime;

    @Override
    public void conenct() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft != null){
            mPayload = aircraft.getPayload();
            mPayload.setMessageCallback(mCallback);
        }
    }

    @Override
    public boolean isConnect() {
        return true;
    }

    @Override
    public void disconenct() {
        if(mPayload != null){
            mPayload.setMessageCallback(null);
        }
    }

    private Payload.HintMessageCallback mCallback = new Payload.HintMessageCallback() {
        @Override
        public void onGetMessage(String msg) {
            mLastUpdateTime = System.currentTimeMillis();
            Log.d("Qimi", "雷达数据：" + msg);
        }
    };

    @Override
    public long getLastDelay() {
        return 0;
    }

    @Override
    public float getLineX() {
        return 0;
    }

    @Override
    public float getLineY() {
        return 0;
    }

    @Override
    public float getLineTreeX1() {
        return 0;
    }

    @Override
    public float getLineTreeY1() {
        return 0;
    }

    @Override
    public float getLineTreeX2() {
        return 0;
    }

    @Override
    public float getLineTreeY2() {
        return 0;
    }

    @Override
    public float getElectricQuantity() {
        return 0;
    }

    @Override
    public void radarCall(RadarCall r) {

    }

}
