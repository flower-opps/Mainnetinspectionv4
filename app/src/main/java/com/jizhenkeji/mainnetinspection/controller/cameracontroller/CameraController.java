package com.jizhenkeji.mainnetinspection.controller.cameracontroller;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.CameraControlStrategy;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.EnterpriseDualControlStrategy;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.H20TControlStrategy;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.Phantom4RTKControlStrategy;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.ProControlStrategy;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy.ZoomControlStrategy;

import java.util.List;
import java.util.Map;
import dji.common.camera.SettingsDefinitions;
import dji.common.product.Model;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class CameraController extends ComponentController {

    private final String TAG = "CameraController";

    public static CameraController getInstance() {
        return Controller.INSTANCE;
    }

    private static class Controller {
        private static final CameraController INSTANCE = new CameraController();
    }

    private CameraController() {}

    private CameraControlStrategy mCameraControlStrategy;

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
        Camera camera = getSuggestionCamera(aircraft.getCameras());
        if(camera == null){
            return CameraControllerError.CAMERA_NOT_EXIST;
        }

        switch (model){
            case MAVIC_2:
            case MAVIC_2_PRO:
            case MAVIC_2_ENTERPRISE_ADVANCED:
                mCameraControlStrategy = new ProControlStrategy(camera);
                break;
            case MAVIC_2_ZOOM:
            case MAVIC_2_ENTERPRISE:
                mCameraControlStrategy = new ZoomControlStrategy(camera);
                break;
            case MATRICE_300_RTK:
                mCameraControlStrategy = new H20TControlStrategy(camera);
                break;
            case PHANTOM_4_RTK:
                mCameraControlStrategy = new Phantom4RTKControlStrategy(camera);
                break;
            case MAVIC_2_ENTERPRISE_DUAL:
                mCameraControlStrategy = new EnterpriseDualControlStrategy(camera);
                break;
            default:
                return CameraControllerError.UNKNOWN;
        }
        return null;
    }

    public Camera getCamera(){
        return mCameraControlStrategy.getCamera();
    }

    public void startShootPhoto(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith){
        mCameraControlStrategy.startShootPhoto(callbackWith);
    }

    public void setCameraMode(SettingsDefinitions.CameraMode cameraMode, CommonCallback<JZIError> callback){
        mCameraControlStrategy.setCameraMode(cameraMode, callback);
    }

    public void startRecordVideo(CommonCallback<JZIError> callback){
        mCameraControlStrategy.startRecordVideo(callback);
    }

    public void stopRecordVideo(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith){
        mCameraControlStrategy.stopRecordVideo(callbackWith);
    }

    public void setMediaFileCallback(CommonCallback<MediaFile> callback){
        mCameraControlStrategy.setMediaFileCallback(callback);
    }

    public float getDFOV(){
        return mCameraControlStrategy.getDFOV();
    }

    private Camera getSuggestionCamera(List<Camera> cameras){
        if(cameras == null || cameras.isEmpty()){
            return null;
        }
        return cameras.get(0);
    }

}
