package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import dji.sdk.camera.Camera;

/**
 * 御2变焦版摄像设备控制策略
 */
public class ZoomControlStrategy extends SingleLensControlStrategy {

    public ZoomControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    public float getDFOV() {
        return 0;
    }

}
