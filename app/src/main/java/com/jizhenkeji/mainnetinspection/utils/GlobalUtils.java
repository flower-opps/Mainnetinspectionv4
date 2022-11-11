package com.jizhenkeji.mainnetinspection.utils;

import android.content.Context;
import android.content.res.AssetManager;

public class GlobalUtils {

    private static Context mApplication;

    public static void initialize(Context context){
        mApplication = context.getApplicationContext();
    }

    public static Context getApplicationContext(){
        return mApplication;
    }

    public static String getString(int resId){
        return mApplication.getString(resId);
    }

    public static int getInteger(int resId){
        return mApplication.getResources().getInteger(resId);
    }

    public static int getColor(int resId){
        return mApplication.getResources().getColor(resId);
	}

    public static AssetManager getAssets(){
        return mApplication.getAssets();
    }

    public static int dpToPx(float dp){
        final float scale = mApplication.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

}
