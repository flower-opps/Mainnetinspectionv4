package com.jizhenkeji.mainnetinspection.yolo5detc;

import static org.greenrobot.eventbus.EventBus.TAG;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;
import com.jizhenkeji.mainnetinspection.missionexecute.FollowWireMissionHandler;
import com.jizhenkeji.mainnetinspection.missionexecute.LineInspectionPublisher;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class Yolox {

    public void capturePic(){
        FollowWireMissionHandler.onShootWirePhoto();
//        Log.d("yolox", "capturePic: 调用成功！");
    }

    public native boolean loadModel(AssetManager mgr, int modelid);
    public native Bitmap detector(Bitmap bitmap, Bitmap.Config config);

    static {
        System.loadLibrary("yoloxncnn");
    }


}
