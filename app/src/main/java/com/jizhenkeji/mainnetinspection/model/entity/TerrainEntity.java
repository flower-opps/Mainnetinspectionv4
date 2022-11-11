package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jizhenkeji.mainnetinspection.common.TerrainInformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class TerrainEntity implements Serializable {

    private static final long serialVersionUID = 3228023330874863870L;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /* 关联的数据Id */
    public long dataId;

    /* 创建日期 */
    public Date createDate = new Date();

    /* 地形测量数据点集合 */
    public List<TerrainInformation> terrainInformations = new ArrayList<>();

}
