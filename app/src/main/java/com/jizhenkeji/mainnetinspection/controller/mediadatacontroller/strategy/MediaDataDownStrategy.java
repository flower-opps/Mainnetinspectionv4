package com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;

import java.io.OutputStream;

import dji.sdk.media.MediaFile;

public interface MediaDataDownStrategy {

    /**
     * 获取目标照片文件的缩略图
     * @param mediaFile
     * @param outputStream
     * @param callback
     */
    void getThumbnailPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback);

    /**
     * 获取目标照片文件的预览图
     * @param mediaFile
     * @param outputStream
     * @param callback
     */
    void getPreviewPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback);

    /**
     * 获取目标照片文件的原始图片数据
     * @param mediaFile
     * @param outputStream
     * @param callback
     */
    void getRawPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback);

    /**
     * 当释放媒体文件控制组件时，会调用此方法
     */
    void release();

}
