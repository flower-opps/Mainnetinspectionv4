package com.jizhenkeji.mainnetinspection.common;

/**
 * 导线相号：A相、B相、C相
 */
public enum WirePhaseNumber {

    PHASE_A("A相"),

    PHASE_B("B相"),

    PHASE_C("C相"),

    PHASE_LEFTGROUND("左地"),

    PHASE_RIGHTGROUND("右地") ;

    private String phaseNumber;

    WirePhaseNumber(String phaseNumber){
        this.phaseNumber = phaseNumber;
    }

    /**
     * 获取导线相号
     * @param name 字符串描述
     * @return
     */
    public static WirePhaseNumber getWirePhaseNumber(String name){
        switch (name){
            case "A相":
                return PHASE_A;
            case "B相":
                return PHASE_B;
            case "C相":
                return PHASE_C;
            case "左地":
                return PHASE_LEFTGROUND;
            case "右地":
                return PHASE_RIGHTGROUND;
            default:
                return null;
        }
    }

    /**
     * 获取相号的字符串描述
     * @return
     */
    public String getName(){
        return phaseNumber;
    }

}
