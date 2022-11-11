package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import dji.sdk.camera.Camera;

/**
 * 御2专业版摄像设备控制策略
 */
public class ProControlStrategy extends SingleLensControlStrategy {

    public ProControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    public float getDFOV() {
        return 77;
    }

}
