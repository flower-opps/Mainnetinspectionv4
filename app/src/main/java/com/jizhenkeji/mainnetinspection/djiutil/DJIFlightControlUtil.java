package com.jizhenkeji.mainnetinspection.djiutil;

import androidx.annotation.NonNull;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.RTKState;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * 大疆MSDK飞行控制对象工具类
 */
public class DJIFlightControlUtil {

    /**
     * 获取大疆SDK的飞行控制对象
     *
     * @return {@link FlightController}
     */
    public static FlightController getFlightController() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if (aircraft == null) {
            return null;
        }
        FlightController flightController = aircraft.getFlightController();
        if (flightController == null) {
            return null;
        }
        return flightController;
    }

    /**
     * 获取当前无人机经度、纬度、高度
     *
     * @return
     */
    public static LocationCoordinate3D getAircraftLocation() {
        FlightController flightController = getFlightController();
        if (flightController == null) {
            return null;
        }
            FlightControllerState flightControllerState = flightController.getState();
            if (flightControllerState == null) {
                return null;
            }
            return flightControllerState.getAircraftLocation();
    }

    /**
     * 获取当前无人机的偏航方向
     *
     * @return
     */
    public static float getAircraftHeading() {
        FlightController flightController = getFlightController();
        if (flightController == null) {
            return Float.NaN;
        }
        Compass compass = flightController.getCompass();
        if (compass == null) {
            return Float.NaN;
        }
        return compass.getHeading();
    }

    /**
     * 获取当前无人机设备名称
     *
     * @return
     */
    public static String getAircraftModel() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if (aircraft == null || aircraft.getModel() == null) {
            return "";
        }
        return aircraft.getModel().getDisplayName();
    }

}
