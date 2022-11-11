package com.jizhenkeji.mainnetinspection.radar;

public class EmptyRadarManager implements RadarManager {

    @Override
    public void conenct() {

    }

    @Override
    public boolean isConnect() {
        return false;
    }

    @Override
    public void disconenct() {

    }

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
