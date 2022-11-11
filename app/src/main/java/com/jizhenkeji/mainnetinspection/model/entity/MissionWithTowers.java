package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.io.Serializable;
import java.util.List;

public class MissionWithTowers implements Serializable {

    private static final long serialVersionUID = -1285691833221858646L;

    @Embedded
    public MissionEntity missionEntity;

    @Relation(
            parentColumn = "id",
            entityColumn = "missionId"
    )
    public List<TowerEntity> towerEntities;

}
