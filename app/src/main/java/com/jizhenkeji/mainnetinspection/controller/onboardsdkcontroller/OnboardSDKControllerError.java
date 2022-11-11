package com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class OnboardSDKControllerError extends JZIError {

    public static final OnboardSDKControllerError UNSUPPORTED_ENCODING = new OnboardSDKControllerError(1, "Unsupported encoding");

    public static final OnboardSDKControllerError DATA_PIPELINE_ERROR = new OnboardSDKControllerError(2, "Data pipeline");

    public static final OnboardSDKControllerError IO_EXCEPTION = new OnboardSDKControllerError(3, "IO exception");

    public OnboardSDKControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 9 << 16; }

}
