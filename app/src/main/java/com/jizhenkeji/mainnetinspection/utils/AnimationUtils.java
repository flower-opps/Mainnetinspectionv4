package com.jizhenkeji.mainnetinspection.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

public class AnimationUtils {
    /**
     * 控件闪动
     * @param view
     */
    public static void flicker(View view){
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1.0f);
        alphaAnimation.setDuration(200);
        alphaAnimation.setInterpolator(new LinearInterpolator());
        alphaAnimation.setRepeatCount(0);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(alphaAnimation);
    }
    public static void clearAnimation(View view){
        view.clearAnimation();
    }
}
