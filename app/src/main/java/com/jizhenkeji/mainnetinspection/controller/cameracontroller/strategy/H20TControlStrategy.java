package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import dji.common.airlink.PhysicalSource;
import dji.sdk.airlink.AirLink;
import dji.sdk.airlink.OcuSyncLink;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * H20T摄像设备控制策略
 */
public class H20TControlStrategy extends TrebleLensControlStrategy {

    private final float WIDE_LENS_DFOV = 82.9f;

    public H20TControlStrategy(Camera camera) {
        super(camera);
        /* 初始化M300视频源，分发视频流宽带至挂载设备 */
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft != null){
            AirLink airLink = aircraft.getAirLink();
            OcuSyncLink ocuSyncLink = airLink.getOcuSyncLink();
            ocuSyncLink.assignSourceToPrimaryChannel(PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM, null);
        }
    }

    @Override
    public float getDFOV() {
        return WIDE_LENS_DFOV;
    }

}
