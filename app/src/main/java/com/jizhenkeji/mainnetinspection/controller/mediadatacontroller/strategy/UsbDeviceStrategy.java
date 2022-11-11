package com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataControllerError;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dji.sdk.media.MediaFile;

public class UsbDeviceStrategy implements MediaDataDownStrategy {

    private final File useDevicePath;

    private final Executor downExecutor;

    public UsbDeviceStrategy(String path){
        useDevicePath = new File(path);
        downExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void getThumbnailPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        getCompressPhoto(mediaFile, outputStream, 20, callback);
    }

    @Override
    public void getPreviewPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        getCompressPhoto(mediaFile, outputStream, 50, callback);
    }

    @Override
    public void getRawPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        getCompressPhoto(mediaFile, outputStream, 100, callback);
    }

    /**
     * 获取压缩后的图片数据
     * @param mediaFile
     * @param outputStream
     * @param quality
     * @param callback
     */
    private void getCompressPhoto(MediaFile mediaFile, OutputStream outputStream, int quality, CommonCallback<JZIError> callback){
        downExecutor.execute(() -> {
            File targetFile = searchFileByName(useDevicePath, mediaFile.getFileName().toUpperCase());
            if(targetFile == null){
                if(callback != null) callback.onResult(MediaDataControllerError.FILE_NOT_EXIST);
                return;
            }
            try{
                FileInputStream fileInputStream = new FileInputStream(targetFile);
                Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
                if(bitmap == null){
                    if(callback != null) callback.onResult(MediaDataControllerError.FILE_TYPE_ERROR);
                    return;
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                if(callback != null) callback.onResult(null);
            } catch (FileNotFoundException e) {
                if(callback != null) callback.onResult(MediaDataControllerError.FILE_NOT_EXIST);
            }
        });
    }

    @Override
    public void release() {

    }

    /**
     * 遍历对应根目录里面的文件，搜索指定文件名称的文件
     * @param rootDirPath
     * @param fileName
     * @return 如果文件存在，则返回文件对象，否则为null
     */
    private File searchFileByName(File rootDirPath, String fileName){
        if(!rootDirPath.exists()){
            return null;
        }
        File[] childFiles = rootDirPath.listFiles();
        if(childFiles == null || childFiles.length == 0){
            return null;
        }
        for(File childFile:childFiles){
            if(childFile.isFile() && childFile.getName().equals(fileName)){
                return childFile;
            }
            if(childFile.isDirectory()){
                File matchFile = searchFileByName(childFile, fileName);
                if(matchFile != null){
                    return matchFile;
                }
            }
        }
        return null;
    }

}
