#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolox.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "opencv2/imgcodecs.hpp"
#include "bitmap_utils.h"

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolox* g_yolox = 0;
static ncnn::Mutex lock;
static double last_capture_time = 0;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jizhenkeji_mainnetinspection_yolo5detc_Yolox_loadModel(JNIEnv *env, jobject thiz,
                                                                jobject assetManager, jint modelid) {
    // TODO: implement loadModel()
    if (modelid < 0 || modelid > 6)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
            {
                    "yolox",
                    "jgbv1-opt",
                    "yolox-tiny",
            };

    const int target_sizes[] =
            {
                    416,
                    416,
            };

    const float mean_vals[][3] =
            {
                    {255.f * 0.485f, 255.f * 0.456, 255.f * 0.406f},
                    {255.f * 0.485f, 255.f * 0.456, 255.f * 0.406f},
            };

    const float norm_vals[][3] =
            {
                    {1 / (255.f * 0.229f), 1 / (255.f * 0.224f), 1 / (255.f * 0.225f)},
                    {1 / (255.f * 0.229f), 1 / (255.f * 0.224f), 1 / (255.f * 0.225f)},
            };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];

    ncnn::MutexLockGuard g(lock);
    if (!g_yolox)
        g_yolox = new Yolox();
    g_yolox->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid],
                  false);

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_jizhenkeji_mainnetinspection_yolo5detc_Yolox_detector(JNIEnv *env, jobject thiz,
                                                               jobject bitmap, jobject config) {
    // TODO: implement detector()
    double start_time = ncnn::get_current_time();
    cv::Mat src;
    if(bitmap2Mat(env, bitmap, &src)<0){
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "CAN NOT COVERT BITMAP TO MAT!!!!");
        return nullptr;
    }
    cv::cvtColor(src, src, cv::COLOR_BGR2RGB);
    __android_log_print(ANDROID_LOG_ERROR, "ncnn", "img_h: %d, img_w: %d", src.rows, src.cols);
    std::vector<Object> objects;
    g_yolox->detect(src, objects, 0.6);
    __android_log_print(ANDROID_LOG_ERROR, "ncnn", "find: %d", objects.size());
    double elasped = ncnn::get_current_time() - start_time;
    cv::Mat out(src.rows, src.cols, CV_8UC4, cv::Scalar(0,0,0,0));
    if (!objects.empty()){
//        double current_captur_time = ncnn::get_current_time();
//        if ((current_captur_time - last_capture_time)>2000){
        jclass jcl = env->GetObjectClass(thiz);
        jmethodID mid = env->GetMethodID(jcl, "capturePic", "()V");
        env->CallVoidMethod(thiz, mid);
//            last_capture_time = current_captur_time;
//        }
    }
    g_yolox->draw(out, objects, elasped);
    cvtColor(out, out, cv::COLOR_BGRA2RGBA);
    return createBitmap(env, out, config, true);
}