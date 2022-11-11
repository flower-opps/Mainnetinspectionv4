package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;

import java.io.Serializable;
import java.util.Date;
import dji.sdk.media.MediaFile;

@Entity
public class PhotoEntity implements Serializable {

    private static final long serialVersionUID = -7238976703032564123L;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /* 关联的数据Id */
    public long dataId;

    /* 照片名称 */
    public String name;

    /* 照片描述 */
    public String description;

    /* 创建日期 */
    public Date createDate = new Date();

    /* 照片的位置信息 */
    public Location location;

    /* 对应的DJIMsdk的媒体文件对象 */
    public MediaFile mediaFile;

    /* 对应的照片类型 */
    public CameraMediaFileType mediaFileType;

}