package com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;
import com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.command.OnboardSDKCommand;
import com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.parser.OnboardSDKDataParser;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import dji.sdk.flightcontroller.FlightController;

public class OnboardSDKController extends ComponentController {

    private final String TAG = "OnboardSDKController";

    private final int SEND_BUFFER_SIZE = 300;

    public static OnboardSDKController getInstance() {
        return Controller.INSTANCE;
    }

    private static class Controller {
        private static final OnboardSDKController INSTANCE = new OnboardSDKController();
    }

    private OnboardSDKController() {}

    private FlightController mFlightController;

    private CopyOnWriteArrayList<OnboardSDKDataParser> mOnboardSDKDataParsers = new CopyOnWriteArrayList<>();

    @Override
    public JZIError init() {
        mFlightController = DJIFlightControlUtil.getFlightController();
        if(mFlightController == null){
            return JZIError.NO_CONNECTED_TO_PRODUCT;
        }
        mFlightController.setOnboardSDKDeviceDataCallback((byte[] datas) -> {
            for(OnboardSDKDataParser parser : mOnboardSDKDataParsers){
                parser.onParse(datas);
            }
        });
        return null;
    }

    public void sendString(String data, CommonCallback<JZIError> callback){
        sendString(data, StandardCharsets.UTF_8.name(), callback);
    }

    public void sendString(String data, String charsetName, CommonCallback<JZIError> callback) {
        try{
            sendBytes(data.getBytes(charsetName), callback);
        } catch (UnsupportedEncodingException e) {
            if(callback != null) callback.onResult(OnboardSDKControllerError.UNSUPPORTED_ENCODING);
        }
    }

    public void sendOnboardSDKCommand(OnboardSDKCommand command, CommonCallback<JZIError> callback){
        sendString(command.toString() + "\r\n", StandardCharsets.UTF_8.name(), callback);
    }

    public void addOnboardSDKDataParser(OnboardSDKDataParser parser){
        if(parser == null){
            return;
        }
        mOnboardSDKDataParsers.add(parser);
    }

    public void removeOnboardSDKDataParser(OnboardSDKDataParser parser){
        mOnboardSDKDataParsers.remove(parser);
    }

    /**
     * 发送数据到OSDK，如果单个数据包长度超过100个字节，则进行分包
     * @param data
     * @param callback
     */
    public void sendBytes(byte[] data, CommonCallback<JZIError> callback){
        byte[] willSendData = data.length > 100 ? Arrays.copyOfRange(data, 0, 100) : data;
        mFlightController.sendDataToOnboardSDKDevice(willSendData, null);
        if(data.length > 100){
            sendBytes(Arrays.copyOfRange(data, 100, data.length), null);
        }
        if(callback != null) callback.onResult(null);
    }

}
