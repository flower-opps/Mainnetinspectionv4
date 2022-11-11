package com.jizhenkeji.mainnetinspection.controller.cameracontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class CameraControllerError extends JZIError {

    public static final CameraControllerError UNKNOWN = new CameraControllerError(0, "Unknown");

    public static final CameraControllerError SET_CAMERA_MODE_ERROR = new CameraControllerError(1, "Set camera mode error");

    public static final CameraControllerError TAKE_PHOTO_ERROR = new CameraControllerError(2, "Take photo error");

    public static final CameraControllerError CAMERA_NOT_EXIST = new CameraControllerError(3, "The camera doesn't exist");

    public CameraControllerError(String description){
        super(0, description);
    }

    public CameraControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 2 << 16; }

}
