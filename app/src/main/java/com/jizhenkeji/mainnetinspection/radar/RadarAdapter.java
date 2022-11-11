package com.jizhenkeji.mainnetinspection.radar;

import android.bluetooth.BluetoothDevice;

public class RadarAdapter {

    private static RadarManager mRadarManager;

    public static void initBluetoothRadar(BluetoothDevice device){
        mRadarManager = new RadarV1Manager(device);
    }

    public static void initPlayloadRadar(){
        mRadarManager = new RadarV2Manager();
    }

    public static void initEmptyRadar(){
        mRadarManager = new EmptyRadarManager();
    }

    public static void initOnboardSDKRadar(){
        mRadarManager = new RadarV3Manager();
    }

    public static RadarManager getRadarManager(){
        return mRadarManager;
    }

}
