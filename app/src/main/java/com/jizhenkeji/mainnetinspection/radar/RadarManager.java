package com.jizhenkeji.mainnetinspection.radar;

/**
 * 描述巡检雷达产品的通用功能方法接口
 */
public interface RadarManager {

    /* 连接雷达设备 */
    void conenct();

    /* 判断雷达是否连接 */
    boolean isConnect();

    /* 断开连接雷达设备 */
    void disconenct();

    /* 获取雷达数据最新获取延迟 */
    long getLastDelay();

    /* 水平离线距离 */
    float getLineX();

    /* 垂直离线距离 */
    float getLineY();

    /* 水平离前树障距离 */
    float getLineTreeX1();

    /* 垂直离前树障距离 */
    float getLineTreeY1();

    /* 水平离后树障距离 */
    float getLineTreeX2();

    /* 垂直离后树障距离 */
    float getLineTreeY2();

    /* 剩余电量 */
    float getElectricQuantity();

    void radarCall(RadarCall r);

    interface RadarCall {
        void radarDataCallBack(float x,float y,float electric);
    }
}
