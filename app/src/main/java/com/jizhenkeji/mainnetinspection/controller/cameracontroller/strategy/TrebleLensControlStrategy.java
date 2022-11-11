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
 * 三镜头摄像设备控制策略
 */
public abstract class TrebleLensControlStrategy extends CameraControlStrategy {

    private LinkedList<CommonCallbackWith<Map<CameraMediaFileType, MediaFile>>> mShootPhotoCallbacks = new LinkedList<>();

    private LinkedList<CommonCallbackWith<Map<CameraMediaFileType, MediaFile>>> mRecordVideoCallbacks = new LinkedList<>();

    private Map<CameraMediaFileType, MediaFile> mPhotoFileTempContainer = new HashMap<>();

    private Map<CameraMediaFileType, MediaFile> mVideoFileTempContainer = new HashMap<>();

    public TrebleLensControlStrategy(Camera camera) {
        super(camera);
    }

    @Override
    protected void onMediaFileCallback(MediaFile mediaFile) {
        super.onMediaFileCallback(mediaFile);
        switch (mediaFile.getMediaType()){
            case JPEG:
                onPhotoFileCallback(mediaFile);
                break;
            case MP4:
                onVideoFileCallback(mediaFile);
                break;
        }
    }

    protected void onPhotoFileCallback(MediaFile mediaFile){
        String mediaFileName = mediaFile.getFileName();
        if(mediaFileName.contains("THRM")){
            mPhotoFileTempContainer.put(CameraMediaFileType.THERMAL, mediaFile);
        }else if(mediaFileName.contains("ZOOM")){
            mPhotoFileTempContainer.put(CameraMediaFileType.ZOOM, mediaFile);
        }else if(mediaFileName.contains("WIDE")){
            mPhotoFileTempContainer.put(CameraMediaFileType.WIDE, mediaFile);
        }
        if(mPhotoFileTempContainer.size() == 3){
            CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith = mShootPhotoCallbacks.getFirst();
            if(callbackWith != null){
                callbackWith.onSuccess(mPhotoFileTempContainer);
            }
            mPhotoFileTempContainer = new HashMap<>();
        }
    }

    protected void onVideoFileCallback(MediaFile mediaFile){
        String mediaFileName = mediaFile.getFileName();
        if(mediaFileName.contains("THRM")){
            mVideoFileTempContainer.put(CameraMediaFileType.THERMAL, mediaFile);
        }else if(mediaFileName.contains("ZOOM")){
            mVideoFileTempContainer.put(CameraMediaFileType.ZOOM, mediaFile);
        }else if(mediaFileName.contains("WIDE")){
            mVideoFileTempContainer.put(CameraMediaFileType.WIDE, mediaFile);
        }
        if(mVideoFileTempContainer.size() == 3){
            CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith = mRecordVideoCallbacks.getFirst();
            if(callbackWith != null){
                callbackWith.onSuccess(mVideoFileTempContainer);
            }
            mVideoFileTempContainer = new HashMap<>();
        }
    }

    @Override
    public void startShootPhoto(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith) {
        getCamera().startShootPhoto((DJIError djiError) -> {
            if(djiError == null){
                mShootPhotoCallbacks.add(callbackWith);
            }else{
                if(callbackWith != null) callbackWith.onFailure(new CameraControllerError(djiError.getDescription()));
            }
        });
    }

    @Override
    public void startRecordVideo(CommonCallback<JZIError> callback) {
        getCamera().startRecordVideo((DJIError djiError) -> {
            if(djiError == null){
                if(callback != null) callback.onResult(null);
            }else{
                if(callback != null) callback.onResult(new CameraControllerError(djiError.getDescription()));
            }
        });
    }

    @Override
    public void stopRecordVideo(CommonCallbackWith<Map<CameraMediaFileType, MediaFile>> callbackWith) {
        getCamera().stopRecordVideo((DJIError djiError) -> {
            if(djiError == null){
                mRecordVideoCallbacks.add(callbackWith);
            }else{
                if(callbackWith != null) callbackWith.onFailure(new JZIError(djiError.getDescription()));
            }
        });
    }

}
