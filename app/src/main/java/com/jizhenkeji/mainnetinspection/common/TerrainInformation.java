package com.jizhenkeji.mainnetinspection.common;

import com.jizhenkeji.mainnetinspection.common.Location;

import java.io.Serializable;

/**
 * 巡检任务过程中的地形数据
 */
public class TerrainInformation implements Serializable {

    private static final long serialVersionUID = 6974662684673905325L;

    /* 无人机的位置信息 */
    public Location aircraftLocation;

    /* 底部距离障碍物距离 */
    public float barrierDistance;

    /* 导线垂直距离障碍物距离 */
    public float wireBarrierDistance;

    /* 导线高度 */
    public float wireHeight;

    /* 小号塔距离 */
    public float trumpetTowerDistance;

}
