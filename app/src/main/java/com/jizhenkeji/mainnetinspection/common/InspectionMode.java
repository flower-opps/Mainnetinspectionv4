package com.jizhenkeji.mainnetinspection.common;

/**
 * 巡检任务模式：导线模式、树障模式、线树模式
 */
public enum InspectionMode {

    WIRE_MODE("导线模式"),

    TREE_MODE("树障模式"),

    WIRE_TREE_MODE("线树模式");

    private String modeName;

    public String getName(){
        return modeName;
    }

    InspectionMode(String name){
        this.modeName = name;
    }

    /**
     * 获取巡检模式
     * @param name 字符串描述
     * @return
     */
    public static InspectionMode getInspectionMode(String name){
        switch (name){
            case "导线模式":
                return WIRE_MODE;
            case "树障模式":
                return TREE_MODE;
            case "线树模式":
                return WIRE_TREE_MODE;
            default:
                return null;
        }
    }

}
