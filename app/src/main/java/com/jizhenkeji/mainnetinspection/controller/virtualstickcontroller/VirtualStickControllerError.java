package com.jizhenkeji.mainnetinspection.controller.virtualstickcontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class VirtualStickControllerError extends JZIError {

    public static final VirtualStickControllerError UNKNOWN = new VirtualStickControllerError(0, "Unknown");

    public static final VirtualStickControllerError NOT_IN_FLIGHT = new VirtualStickControllerError(1, "The aircraft is not in flight");

    public static final VirtualStickControllerError ALREADY_IN_CONTROL = new VirtualStickControllerError(2, "Already in control");

    public static final VirtualStickControllerError NOT_IN_CONTROL = new VirtualStickControllerError(3, "Not in control");

    public static final VirtualStickControllerError TIME_OUT = new VirtualStickControllerError(4, "Timeout");

    public static final VirtualStickControllerError NOT_IN_PAUSE_CONTROL = new VirtualStickControllerError(5, "Not in pause control");

    public VirtualStickControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 5 << 16;}

}
