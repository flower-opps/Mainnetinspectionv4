package com.jizhenkeji.mainnetinspection.utils;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.jizhenkeji.mainnetinspection.common.Location;

/**
 * GPS坐标相关工具类
 */
public class GPSUtil {

    private final static double Rc = 6378137;

    private final static double Rj = 6356725;

    /**
     * 计算两个GPS坐标点间距离
     * @param startLatitude 起始点纬度
     * @param startLongitude 起始点经度
     * @param endLatitude 末尾点纬度
     * @param endLongitude 末尾点经度
     * @return 两点间距离 单位：M
     */
    public static float calculateLineDistance(double startLatitude, double startLongitude, double endLatitude, double endLongitude){
        LatLng startPoint = new LatLng(startLatitude, startLongitude);
        LatLng endPoint = new LatLng(endLatitude, endLongitude);
        return AMapUtils.calculateLineDistance(startPoint, endPoint);
    }

    /**
     * 获取两点间相对于正北的角度
     * @param startLatitude
     * @param startLongitude
     * @param endLatitude
     * @param endLongitude
     * @return 输出[-180, 180]范围的角度值，正北为0度，正东为90度，正西为-90度，正南为180度
     */
    public static double getAngleBetweenGpsCoordinate(double startLatitude, double startLongitude, double endLatitude, double endLongitude){
        double aRadLo = startLongitude * Math.PI / 180.;
        double aRadLa = startLatitude * Math.PI / 180.;
        double aEc = Rj + (Rc - Rj) * (90. - startLatitude) / 90.;
        double aEd = aEc * Math.cos(aRadLa);

        double bRadLo = endLongitude * Math.PI / 180.;
        double bRadLa = endLatitude * Math.PI / 180.;
        double bEc = Rj + (Rc - Rj) * (90. - endLatitude) / 90.;
        double bEd = bEc * Math.cos(bRadLa);

        double dx = (bRadLo - aRadLo) * aEd;
        double dy = (bRadLa - aRadLa) * aEc;
        double angle = 0.0;
        angle = Math.atan(Math.abs(dx / dy)) * 180. / Math.PI;
        double dLo = endLongitude - startLongitude;
        double dLa = endLatitude - startLatitude;
        if (dLo > 0 && dLa <= 0) {
            angle = (90. - angle) + 90;
        } else if (dLo <= 0 && dLa < 0) {
            angle = angle + 180.;
        } else if (dLo < 0 && dLa >= 0) {
            angle = (90. - angle) + 270;
        }
        /* 将范围为[0, 360]的偏航角度转换为[-180, 180] */
        if(angle > 180){
            angle = angle - 360;
        }
        return angle;
    }

    /**
     * 计算目标点垂直于两个原点连线的垂直角度
     * @param baseLocation
     * @param anotherLocation
     * @param targetLocation
     * @return
     */
    public static float calculateVerticalAngle(Location baseLocation, Location anotherLocation, Location targetLocation){
        float baseHeading = (float) getAngleBetweenGpsCoordinate(baseLocation.latitude, baseLocation.longitude,
                anotherLocation.latitude, anotherLocation.longitude);
        float targetHeading = (float) getAngleBetweenGpsCoordinate(baseLocation.latitude, baseLocation.longitude,
                targetLocation.latitude, targetLocation.longitude);
        float hasOffsetTargetHeading = calibrateHeading(targetHeading - baseHeading);
        return calibrateHeading(hasOffsetTargetHeading > 0 ? baseHeading - 90 : baseHeading + 90);
    }

    /**
     * 计算水平空间下目标位置坐标
     * @param baseLocation 基准坐标
     * @param relativeYaw 目标相对基准位置的偏航角度 (-180, 180]
     * @param distance 距离目标的直线距离
     * @return
     */
    public static Location calculateTargetLocation(Location baseLocation, float relativeYaw, float distance){
        return calculateTargetLocation(baseLocation, 0, relativeYaw, distance);
    }

    /**
     * 计算三维空间下目标位置坐标
     * @param baseLocation 基准坐标
     * @param relativePitch 目标相对基准位置的俯仰角度
     * @param relativeYaw 目标相对基准位置的偏航角度 (-180, 180]
     * @param distance 距离目标的直线距离
     * @return
     */
    public static Location calculateTargetLocation(Location baseLocation, float relativePitch, float relativeYaw, float distance){
        /* 计算目标点水平方向上相对基准点的直线距离 */
        double relativeHorizontalDistanceToTarget = Math.cos(angleToRadian(relativePitch)) * distance;
        /* 计算目标点垂直方向上相对基准点的直线距离 */
        double relativeVerticalDistanceToTarget = Math.sin(angleToRadian(relativePitch)) * distance;
        /* 计算水平方向上的X/Y轴的距离偏差 */
        double xDistanceOffset = Math.sin(angleToRadian(relativeYaw)) * relativeHorizontalDistanceToTarget;
        double yDistanceOffset = Math.cos(angleToRadian(relativeYaw)) * relativeHorizontalDistanceToTarget;
        /* 计算水平方向上的经纬度偏差 */
        double latitudeOffset = yDistanceOffset / 1.1 * 0.00001;
        double longitudeOffset = xDistanceOffset * 0.00001;
        /* 构建目标位置坐标 */
        Location targetLocation = new Location(
                baseLocation.latitude + latitudeOffset,
                baseLocation.longitude + longitudeOffset,
                (float) (baseLocation.altitude + relativeVerticalDistanceToTarget));
        return targetLocation;
    }

    /**
     * 校准大地坐标角度，将超出范围(-180, 180]的角度值修正
     * @param angle
     * @return
     */
    public static float calibrateHeading(float angle) {
        float res;
        if(angle >= 0){
            res = (angle + 180) % 360 - 180;
        }else {
            res = (angle - 180) % 360 + 180;
        }
        if(res == -180f){
            return 180f;
        }
        return res;
    }

    /**
     * 角度转弧度
     * @param angle
     * @return
     */
    public static double angleToRadian(double angle){
        return angle * Math.PI / 180;
    }

}
