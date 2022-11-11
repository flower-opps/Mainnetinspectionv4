package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import dji.sdk.camera.Camera;

/**
 * Phantom 4 RTK摄像设备控制策略
 */
public class Phantom4RTKControlStrategy extends SingleLensControlStrategy {

    public Phantom4RTKControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    public float getDFOV() {
        return 0;
    }

}
