package com.jizhenkeji.mainnetinspection.controller.mediadatacontroller;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class MediaDataControllerError extends JZIError {

    public static final MediaDataControllerError FILE_NOT_EXIST = new MediaDataControllerError(1, "File not exist");

    public static final MediaDataControllerError FILE_TYPE_ERROR = new MediaDataControllerError(2, "File type error");

    public static final MediaDataControllerError UNCONFIGURED_STRATEGY = new MediaDataControllerError(3, "Unconfigured strategy");

    public MediaDataControllerError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 3 << 16; }

}
