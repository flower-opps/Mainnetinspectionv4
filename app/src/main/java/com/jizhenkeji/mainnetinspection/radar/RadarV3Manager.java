package com.jizhenkeji.mainnetinspection.radar;

import com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.OnboardSDKController;
import com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.parser.StringDataParser;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;

import dji.sdk.flightcontroller.FlightController;

public class RadarV3Manager extends StringDataParser implements RadarManager {

    private boolean isConnect = false;

    @Override
    public void conenct() {
        OnboardSDKController.getInstance().addOnboardSDKDataParser(this);
        isConnect = true;
    }

    @Override
    public boolean isConnect() {
        FlightController flightController = DJIFlightControlUtil.getFlightController();
        return isConnect && flightController != null;
    }

    @Override
    public void disconenct() {
        OnboardSDKController.getInstance().removeOnboardSDKDataParser(this);
        isConnect = false;
    }

    @Override
    protected void onReceive(String data) {
        String[] radarDatas = data.split("[A-F]");
        if(radarDatas.length != 6){
            return;
        }
        mLastUpdateTime = System.currentTimeMillis();

        float lineDistance = Float.valueOf(radarDatas[1])/1000.0f;
        float lineAngle = Float.valueOf(radarDatas[2])/10.0f;
        // float pointCount = Float.valueOf(radarDatas[3]);
        mLineTreeX1 = Float.valueOf(radarDatas[4])/1000.0f;
        mLineTreeY1 = Float.valueOf(radarDatas[5])/1000.0f;

        if(lineDistance > 1 && lineDistance < 6){
            mLineX = (float) (lineDistance * Math.cos(lineAngle / 180.0f * 3.14159f));
            mLineY = (float) (lineDistance * Math.sin(lineAngle / 180.0f * 3.14159f)) * -1f;
        }else{
            mLineX = 0f;
            mLineY = 0f;
        }
    }

    private volatile long mLastUpdateTime;

    @Override
    public long getLastDelay() {
        return System.currentTimeMillis() - mLastUpdateTime;
    }

    private volatile float mLineX;

    @Override
    public float getLineX() {
        return mLineX;
    }

    private volatile float mLineY;

    @Override
    public float getLineY() {
        return mLineY;
    }

    private volatile float mLineTreeX1;

    @Override
    public float getLineTreeX1() {
        return mLineTreeX1;
    }

    private volatile float mLineTreeY1;

    @Override
    public float getLineTreeY1() {
        return mLineTreeY1;
    }

    private volatile float mLineTreeX2;

    @Override
    public float getLineTreeX2() {
        return mLineTreeX2;
    }

    private volatile float mLineTreeY2;

    @Override
    public float getLineTreeY2() {
        return mLineTreeY2;
    }

    @Override
    public float getElectricQuantity() {
        return 0;
    }

    @Override
    public void radarCall(RadarCall r) {

    }

}
