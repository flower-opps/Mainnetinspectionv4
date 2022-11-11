package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import dji.sdk.camera.Camera;

public class EnterpriseDualControlStrategy extends DualLensControlStrategy {

    public EnterpriseDualControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    public float getDFOV() {
        return 0;
    }

}
