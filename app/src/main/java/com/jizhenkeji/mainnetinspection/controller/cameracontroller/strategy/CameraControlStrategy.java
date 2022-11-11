package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraControllerError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;
import java.util.Map;
import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.common.error.DJIError;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;

/**
 * 摄像设备控制策略
 */
public abstract class CameraControlStrategy {

    private Camera mCamera;

    private CommonCallback<MediaFile> mMediaFileCallback;

    public CameraControlStrategy(Camera camera){
        mCamera = camera;
        mCamera.setMediaFileCallback(this::onMediaFileCallback);
    }

    /**
     * 获取大疆云台相机对象
     * @return
     */
    public Camera getCamera(){
        return mCamera;
    }

    /**
     * 设置媒体文件回调接口
     * @param callback
     */
    public void setMediaFileCallback(CommonCallback<MediaFile> callback){
        mMediaFileCallback = callback;
    }

    /**
     * 获取媒体文件回调接口
     * @return
     */
    protected CommonCallback<MediaFile> getMediaFileCallback(){
        return mMediaFileCallback;
    }

    protected void onMediaFileCallback(MediaFile mediaFile){
        if(mMediaFileCallback != null) {
            mMediaFileCallback.onResult(mediaFile);
        }
    }

    public abstract void startShootPhoto(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith);

    public abstract void startRecordVideo(CommonCallback<JZIError> callback);

    public abstract void stopRecordVideo(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith);

    public abstract float getDFOV();

    public void setCameraMode(CameraMode cameraMode, CommonCallback<JZIError> callback){
        mCamera.setMode(cameraMode, (DJIError djiError) -> {
            if(callback != null) callback.onResult(djiError == null ? null : CameraControllerError.SET_CAMERA_MODE_ERROR);
        });
    }

}
