package com.jizhenkeji.mainnetinspection.common;

import java.io.Serializable;

/**
 * 位置信息对象实体
 */
public class Location implements Serializable {

    private static final long serialVersionUID = -3285276873211109756L;

    public final double latitude;

    public final double longitude;

    public final float altitude;

    public Location(double latitude, double longitude, float altitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

}
