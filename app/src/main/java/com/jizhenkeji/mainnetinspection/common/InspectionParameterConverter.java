package com.jizhenkeji.mainnetinspection.common;

import androidx.databinding.InverseMethod;

/**
 * 巡检参数实体数据与UI组件显示数据之间的类型装换类
 */
public class InspectionParameterConverter {

    /**
     * 导线巡检过程的飞行速度与进度条的进度数值之间的转换方法
     * @param value 飞行速度
     * @return
     */
    @InverseMethod("progressIntToSpeedFloat")
    public static int speedFloatToProgressInt(float value) {
        return (int) ((value - 0.5) / 0.5);
    }

    /**
     * 导线巡检过程塔号与进度之间的转换
     * @param value 塔号
     * @return
     */
    @InverseMethod("starTowerProgressInt")
    public static int starTowerProgressInt(int value) {
        return value;
    }

    public static float progressIntToSpeedFloat(int value) {
        return value * 0.5f + 0.5f;
    }

    /**
     * 导线巡检过程中水平方向上离线距离与进度条的进度数值之间的转换方法
     * @param value
     * @return
     */
    @InverseMethod("ProgressIntToHorizontalDistanceFloat")
    public static int horizontalDistanceFloatToProgressInt(float value){
        return (int) ((value - 2) / 0.5);
    }

    public static float ProgressIntToHorizontalDistanceFloat(int value){
        return value * 0.5f + 2;
    }

    /**
     * 导线巡检过程中垂直方向上离线距离与进度条的进度数值之间的转换方法
     * @param value
     * @return
     */
    @InverseMethod("progressIntToVerticalDistanceFloat")
    public static int verticalDistanceFloatToProgressInt(float value){
        return (int) (value * 10);
    }

    public static float progressIntToVerticalDistanceFloat(int value){
        return (float) (value * 0.1);
    }

    /**
     * 导线巡检过程中树障报警阈值与进度条数值之间的转换方法
     * @param value
     * @return
     */
    @InverseMethod("ProgressIntTotreeBarrierDistanceInt")
    public static int treeBarrierDistanceIntToProgressInt(int value){
        return value - 2;
    }

    public static int ProgressIntTotreeBarrierDistanceInt(int value){
        return value + 2;
    }

}
