package com.jizhenkeji.mainnetinspection.controller.cameracontroller;

/**
 * 定义大疆摄像设备所生成的媒体文件类型
 */
public enum CameraMediaFileType {

    VISUAL("visual"),

    THERMAL("thermal"),

    WIDE("wide"),

    ZOOM("zoom");

    private String typeName;

    public String getTypeName(){
        return typeName;
    }

    CameraMediaFileType(String typeName){
        this.typeName = typeName;
    }

}
