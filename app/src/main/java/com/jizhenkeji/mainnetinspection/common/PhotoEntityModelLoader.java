package com.jizhenkeji.mainnetinspection.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;
import java.nio.ByteBuffer;

public class PhotoEntityModelLoader implements ModelLoader<PhotoEntity, ByteBuffer> {

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull PhotoEntity photoEntity, int width, int height, @NonNull Options options) {
        Key diskCacheKey = new ObjectKey(photoEntity.mediaFile.getFileName());
        return new LoadData<>(diskCacheKey, new PhotoEntityDataFetcher(photoEntity));
    }

    @Override
    public boolean handles(@NonNull PhotoEntity wirePhotoEntity) {
        if(wirePhotoEntity.mediaFile == null){
            return false;
        }
        return true;
    }

}
