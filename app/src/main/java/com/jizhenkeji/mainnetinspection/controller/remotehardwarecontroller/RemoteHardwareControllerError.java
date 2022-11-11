package com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class RemoteHardwareControllerError extends JZIError {

    public RemoteHardwareControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 4 << 16; }

}
