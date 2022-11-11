package com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy;

import android.graphics.Bitmap;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;

import java.io.IOException;
import java.io.OutputStream;

import dji.common.error.DJIError;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.MediaFile;

public class WirelessStrategy implements MediaDataDownStrategy {

    @Override
    public void getThumbnailPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        mediaFile.fetchThumbnail((DJIError djiError) -> {
            if(djiError != null){
                if(callback != null) callback.onResult(new JZIError(djiError.getDescription()));
                return;
            }
            Bitmap thumbnailPhoto = mediaFile.getThumbnail();
            thumbnailPhoto.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            if(callback != null) callback.onResult(null);
        });
    }

    @Override
    public void getPreviewPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        mediaFile.fetchPreview((DJIError djiError) -> {
            if(djiError != null){
                if(callback != null) callback.onResult(new JZIError(djiError.getDescription()));
                return;
            }
            Bitmap previewPhoto = mediaFile.getPreview();
            previewPhoto.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            if(callback != null) callback.onResult(null);
        });
    }

    @Override
    public void getRawPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        mediaFile.fetchFileByteData(0, new DJIDownloadListener() {
            @Override
            public void onSuccess(String s) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
                if(callback != null) callback.onResult(null);
            }

            @Override
            public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {}
            }

            @Override
            public void onFailure(DJIError djiError) {
                if(callback != null) callback.onResult(new JZIError(djiError.getDescription()));
            }
        });
    }

    @Override
    public void release() {

    }

    public abstract class DJIDownloadListener implements DownloadListener<String> {
        @Override
        public void onStart() {}
        @Override
        public void onRateUpdate(long l, long l1, long l2) {}
        @Override
        public void onProgress(long l, long l1) {}

        @Override
        public abstract void onSuccess(String s);

        @Override
        public abstract void onRealtimeDataUpdate(byte[] bytes, long l, boolean b);

        @Override
        public abstract void onFailure(DJIError djiError);

    }

}
