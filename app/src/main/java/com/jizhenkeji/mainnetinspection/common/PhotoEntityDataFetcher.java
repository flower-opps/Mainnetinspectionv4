package com.jizhenkeji.mainnetinspection.common;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;
import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PhotoEntityDataFetcher implements DataFetcher<ByteBuffer> {

    private final PhotoEntity mPhotoEntity;

    public PhotoEntityDataFetcher(PhotoEntity photoEntity){
        mPhotoEntity = photoEntity;
    }

    private ByteArrayOutputStream mTempOutputStream;

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {
        mTempOutputStream = new ByteArrayOutputStream();
        MediaDataController.getInstance().getPreviewPhoto(mPhotoEntity.mediaFile, mTempOutputStream, (JZIError jziError) -> {
            if(jziError == null){
                ByteBuffer byteBuffer = ByteBuffer.wrap(mTempOutputStream.toByteArray());
                callback.onDataReady(byteBuffer);
            }else{
                callback.onLoadFailed(new Exception(jziError.getDescription()));
            }
        });
    }

    @Override
    public void cleanup() {
        try {
            mTempOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        // TODO 留空
    }

    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
        return ByteBuffer.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }

}
