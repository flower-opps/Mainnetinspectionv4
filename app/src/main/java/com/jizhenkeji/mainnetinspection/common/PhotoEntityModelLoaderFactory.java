package com.jizhenkeji.mainnetinspection.common;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;

import java.nio.ByteBuffer;


public class PhotoEntityModelLoaderFactory implements ModelLoaderFactory<PhotoEntity, ByteBuffer> {

    @NonNull
    @Override
    public ModelLoader<PhotoEntity, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new PhotoEntityModelLoader();
    }

    @Override
    public void teardown() {

    }

}
