package com.jizhenkeji.mainnetinspection.common;

public enum FlightDirection {

    /**
     * 向左巡检
     */
    LEFT(-1),

    /**
     * 向右巡检
     */
    RIGHT(1),

    /**
     * 机头角度计算异常
     */
    UNKNOWN(0);

    private int flightDirection;

    FlightDirection(int flightDirection){
        this.flightDirection = flightDirection;
    }

    public int getDirection(){
        return flightDirection;
    }

}
