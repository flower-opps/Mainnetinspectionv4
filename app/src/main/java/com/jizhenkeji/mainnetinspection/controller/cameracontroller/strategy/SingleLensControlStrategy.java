package com.jizhenkeji.mainnetinspection.controller.cameracontroller.strategy;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraControllerError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import dji.common.error.DJIError;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;

/**
 * 单镜头设备的控制策略
 */
public abstract class SingleLensControlStrategy extends CameraControlStrategy {

    private LinkedList<CommonCallbackWith<Map<CameraMediaFileType, MediaFile>>> mShootPhotoCallbacks = new LinkedList<>();

    public SingleLensControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    protected void onMediaFileCallback(MediaFile mediaFile) {
        super.onMediaFileCallback(mediaFile);
        switch (mediaFile.getMediaType()){
            case JPEG:
                onPhotoFileCallback(mediaFile);
                break;
        }
    }

    protected void onPhotoFileCallback(MediaFile mediaFile){
        if(mShootPhotoCallbacks.isEmpty()){
            return;
        }
        CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callback = mShootPhotoCallbacks.poll();
        if(callback == null){
            return;
        }
        Map<CameraMediaFileType, MediaFile> mediaFileMap = new HashMap<>();
        mediaFileMap.put(CameraMediaFileType.VISUAL, mediaFile);
        callback.onSuccess(mediaFileMap);
    }

    @Override
    public void startShootPhoto(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith) {
        getCamera().startShootPhoto((DJIError djiError) -> {
            if(djiError == null){
                mShootPhotoCallbacks.add(callbackWith);
            }else{
                if(callbackWith != null) callbackWith.onFailure(CameraControllerError.TAKE_PHOTO_ERROR);
            }
        });
    }

    @Override
    public void startRecordVideo(CommonCallback<JZIError> callback) {
        // TODO
    }

    @Override
    public void stopRecordVideo(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith) {
        // TODO
    }

}
