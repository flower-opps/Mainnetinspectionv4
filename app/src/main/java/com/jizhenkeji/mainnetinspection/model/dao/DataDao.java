package com.jizhenkeji.mainnetinspection.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import com.jizhenkeji.mainnetinspection.model.entity.DataEntity;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.TerrainEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;
import java.util.List;

@Dao
public interface DataDao {

    @Insert
    long insertData(DataEntity dataEntity);

    @Transaction
    @Query("SELECT * FROM DataEntity")
    LiveData<List<DataWithMetadata>> getAllDatas();

    @Transaction
    @Query("SELECT * FROM DataEntity Where missionId=:missionId")
    LiveData<List<DataWithMetadata>> getForIdDatas(long missionId);

    @Insert
    long insertWirePhoto(WirePhotoEntity wirePhotoEntity);

    @Insert
    long insertTreePhoto(TreePhotoEntity treePhotoEntity);

    @Insert
    long insertTerrain(TerrainEntity terrainEntity);

    @Delete
    void deleteData(DataEntity dataEntity);

    @Delete
    void deleteWirePhoto(WirePhotoEntity wirePhotoEntity);

    @Delete
    void deleteTreePhoto(TreePhotoEntity treePhotoEntity);

    @Delete
    void deleteTerrain(TerrainEntity terrainEntity);

}
