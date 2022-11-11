package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jizhenkeji.mainnetinspection.common.Location;

import java.io.Serializable;
import java.util.Date;

@Entity
public class DataEntity implements Serializable {

    private static final long serialVersionUID = -6329059913554604385L;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /* 关联的任务ID */
    public long missionId;

    /* 关联的任务名称 */
    public String missionName;

    /* 电压等级 */
    public String voltageLevel;

    /* 管辖班组 */
    public String manageClassName;

    /* 创建日期 */
    public Date createDate = new Date();

    /* 相号 */
    public String phaseNumber;

    /* 无人机名称 */
    public String aircraftName;

    /* 数据对应的位置信息 */
    public Location location;

    /* 起始塔号 */
    public int  startTowerNum;

    /* 起始点位置信息 */
    public Location startPointLocation;

    /* 终止塔号 */
    public int endTowerNum;

    /* 终止点位置信息 */
    public Location endPointLocation;

    /* 树障警报阈值 */
    public int treeBarrierThreshold;

}
