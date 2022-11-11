package com.jizhenkeji.mainnetinspection;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.jizhenkeji.mainnetinspection.common.PhotoEntityModelLoaderFactory;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;

import java.nio.ByteBuffer;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(PhotoEntity.class, ByteBuffer.class, new PhotoEntityModelLoaderFactory());
    }

}
