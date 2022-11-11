package com.jizhenkeji.mainnetinspection.common;


public class JZIError {

    public static final JZIError UNKNOWN = new JZIError(1, "Unknown error");

    public static final JZIError NO_CONNECTED_TO_PRODUCT = new JZIError(2, "No connected to product");

    public static final JZIError CAN_GET_MODEL = new JZIError(3, "An exception occurred when getting the model");

    public static final JZIError FAIL_TO_CONNECT_OSDK = new JZIError(4, "Fail to connect Osdk");

    public static final JZIError FAIL_TO_BUILD_REPORT = new JZIError(5, "Fail to build report");

    private final int errorCode;

    private final String description;

    public JZIError(String description){
        this(0, description);
    }

    public JZIError(int errorCode, String description){
        this.errorCode = errorCode | getErrorMarkCode();
        this.description = description;
    }

    protected int getErrorMarkCode(){ return 1 << 16; }

    public int getErrorCode(){
        return errorCode;
    }

    public String getDescription(){
        return description;
    }

}
