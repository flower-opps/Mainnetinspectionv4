package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jizhenkeji.mainnetinspection.common.Location;

import java.io.Serializable;
import java.util.Date;


@Entity
public class MissionEntity implements Serializable {

    private static final long serialVersionUID = -5749443500873464485L;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /* 创建日期 */
    public Date createDate = new Date();

    /* 任务名称 */
    public String name;

    /* 电压等级 */
    public String voltageLevel;

    /* 管辖班组 */
    public String manageClassName;

}
