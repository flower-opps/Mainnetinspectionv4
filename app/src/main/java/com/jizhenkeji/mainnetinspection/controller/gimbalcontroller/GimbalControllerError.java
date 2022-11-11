package com.jizhenkeji.mainnetinspection.controller.gimbalcontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class GimbalControllerError extends JZIError {

    public static final GimbalControllerError UNKNOWN = new GimbalControllerError(0, "Unknown");

    public static final GimbalControllerError GIMBAL_NOT_EXIST = new GimbalControllerError(1, "The gimbal doesn't exist");

    public static final GimbalControllerError GIMBAL_ROTATE_ERROR = new GimbalControllerError(2, "Gimbal rotate error");

    public GimbalControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 8 << 16; }

}
