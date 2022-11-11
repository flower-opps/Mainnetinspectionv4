//
// Created by ayuqi on 2022/6/22.
//

#ifndef JNIDEMO_BITMAP_UTILS_H
#define JNIDEMO_BITMAP_UTILS_H

#include <android/bitmap.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;//Mat

extern "C" {
    /**
     * Bitmap 转矩阵
     * @param env JNI环境
     * @param bitmap Bitmap对象
     * @param mat 图片矩阵
     * @param needPremultiplyAlpha 是否前乘透明度
     */
    int bitmap2Mat(JNIEnv *env, jobject bitmap, Mat *mat, bool needPremultiplyAlpha = false){
        AndroidBitmapInfo info;
        void *pixels = 0;
        Mat &dst = *mat;
        //获取信息和一些断言
        if(AndroidBitmap_getInfo(env, bitmap, &info) < 0){
            return -1;
        }//获取Bitmap信息
        if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888//图片格式RGBA_8888 或RGB_565
                  && info.format != ANDROID_BITMAP_FORMAT_RGB_565){
            return -1;
        }
        if(AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0){
            return -1;
        }
        if(!pixels){
            return -1;
        }

        dst.create(info.height, info.width, CV_8UC4);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (needPremultiplyAlpha) {
                cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            } else {
                tmp.copyTo(dst);
            }
        } else {
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return 1;
    }

    /**
     * 矩阵转Bitmap
     * @param env JNI环境
     * @param mat 图片矩阵
     * @param bitmap Bitmap对象
     * @param needPremultiplyAlpha 是否前乘透明度
     */
    void mat2Bitmap(JNIEnv *env, Mat mat, jobject bitmap, bool needPremultiplyAlpha = false){
        AndroidBitmapInfo info;
        void *pixels = 0;
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);//获取Bitmap信息
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888//图片格式RGBA_8888 或RGB_565
                  || info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(mat.dims==2&&info.height==(uint32_t)mat.rows && info.width==(uint32_t)mat.cols);
        CV_Assert(mat.type()==CV_8UC1||mat.type()==CV_8UC3||mat.type()==CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            switch (mat.type()){
                case CV_8UC1:
                    cvtColor(mat,tmp,COLOR_GRAY2RGBA);
                    break;
                case CV_8UC3:
                    cvtColor(mat,tmp,COLOR_BGR2RGBA);
                    break;
                case CV_8UC4:
                    cvtColor(mat,tmp,COLOR_RGBA2mRGBA);
                    if (needPremultiplyAlpha) {
                        cvtColor(mat, tmp, COLOR_RGBA2mRGBA);
                    } else {
                        mat.copyTo(tmp);
                    }
                    break;
                default:break;
            }
        } else {
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            switch (mat.type()){
                case CV_8UC1:
                    cvtColor(mat,tmp,COLOR_GRAY2BGR565);
                    break;
                case CV_8UC3:
                    cvtColor(mat,tmp,COLOR_RGB2BGR565);
                    break;
                case CV_8UC4:
                    cvtColor(mat,tmp,COLOR_RGBA2BGR565);
                    break;
                default:break;
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }

    /**
     *
     * 创建Bitmap
     * @param env JNI环境
     * @param src 矩阵
     * @param config Bitmap配置
     * @return Bitmap对象
     */
    jobject createBitmap(JNIEnv *env, Mat src, jobject config, bool needPremultiplyAlpha=false){
        jclass java_bitmap_class=(jclass)env->FindClass("android/graphics/Bitmap");//类名
        jmethodID mid=env->GetStaticMethodID(java_bitmap_class,"createBitmap",//获取方法
                                             "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        jobject bitmap=env->CallStaticObjectMethod(java_bitmap_class,mid,src.cols,src.rows,config);
        mat2Bitmap(env,src,bitmap, false);
        return  bitmap;
    }
}

#endif //JNIDEMO_BITMAP_UTILS_H
