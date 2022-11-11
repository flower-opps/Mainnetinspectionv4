package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.io.Serializable;
import java.util.List;

public class DataWithMetadata implements Serializable {

    private static final long serialVersionUID = -6880831356678199291L;

    @Embedded
    public DataEntity dataEntity;

    @Relation(
            parentColumn = "id",
            entityColumn = "dataId"
    )
    public List<WirePhotoEntity> wirePhotoEntities;

    @Relation(
            parentColumn = "id",
            entityColumn = "dataId"
    )
    public List<TreePhotoEntity> treePhotoEntities;

    @Relation(
            parentColumn = "id",
            entityColumn = "dataId"
    )
    public TerrainEntity terrainEntity;

}
