package com.jizhenkeji.mainnetinspection.controller.gimbalcontroller;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;

import java.util.List;

import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.keysdk.GimbalKey;
import dji.keysdk.KeyManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class GimbalController extends ComponentController {

    private final String TAG = "CameraController";

    public static GimbalController getInstance() {
        return Controller.INSTANCE;
    }

    private static class Controller {
        private static final GimbalController INSTANCE = new GimbalController();
    }

    private GimbalController() {}

    private Gimbal mGimbal;

    @Override
    public JZIError init() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft == null){
            return JZIError.NO_CONNECTED_TO_PRODUCT;
        }
        Model model = aircraft.getModel();
        if(model == null){
            return JZIError.CAN_GET_MODEL;
        }
        Gimbal gimbal = getSuggestionGimbal(aircraft.getGimbals());
        if(gimbal == null){
            return GimbalControllerError.GIMBAL_NOT_EXIST;
        }
        mGimbal = gimbal;
        return null;
    }

    /**
     * 旋转云台Pitch到指定的角度
     * @param absoluteAngle
     */
    public void rotatePitchTo(float absoluteAngle, CommonCallback<JZIError> callback){
        Rotation rotation = new Rotation.Builder()
                .mode(RotationMode.ABSOLUTE_ANGLE)
                .pitch(absoluteAngle)
                .build();
        mGimbal.rotate(rotation, (DJIError djiError) -> {
            if(djiError != null){
                if(callback != null) callback.onResult(GimbalControllerError.GIMBAL_ROTATE_ERROR);
            }else{
                if(callback != null) callback.onResult(null);
            }
        });
    }

    /**
     * 获取云台俯仰角度
     * @return
     */
    public float getPitchAngle(){
        /* 获取云台角度 */
        KeyManager keyManager = DJISDKManager.getInstance().getKeyManager();
        Attitude attitude = (Attitude) keyManager.getValue(GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES));
        if(attitude == null){
            return 0;
        }
        return attitude.getPitch();
    }

    /**
     * 获取云台偏航角度
     * @return
     */
    public float getYawAngle(){
        /* 获取云台角度 */
        KeyManager keyManager = DJISDKManager.getInstance().getKeyManager();
        Attitude attitude = (Attitude) keyManager.getValue(GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES));
        if(attitude == null){
            return 0;
        }
        return attitude.getYaw();
    }

    private Gimbal getSuggestionGimbal(List<Gimbal> gimbals){
        if(gimbals == null || gimbals.isEmpty()){
            return null;
        }
        return gimbals.get(0);
    }

}
