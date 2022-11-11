package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jizhenkeji.mainnetinspection.common.Location;

import java.io.Serializable;
import java.util.Date;

@Entity
public class TowerEntity implements Serializable {

    private static final long serialVersionUID = -7591801350360161064L;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /* 关联的任务ID */
    public long missionId;

    /* 杆塔号 */
    public long towerNum;

    /* 创建日期 */
    public Date createDate = new Date();

    /* 坐标信息 */
    public Location location;

}
