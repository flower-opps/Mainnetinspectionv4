package com.jizhenkeji.mainnetinspection.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.jizhenkeji.mainnetinspection.model.entity.MissionEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;

import java.util.List;

@Dao
public interface MissionDao {

    @Insert
    long insertMission(MissionEntity missionEntity);

    @Transaction
    @Query("SELECT * FROM MissionEntity")
    LiveData<List<MissionWithTowers>> getAllMissions();

    @Insert
    long insertTower(TowerEntity towerEntity);

    @Update
    void updateTower(TowerEntity towerEntity);

    @Query("SELECT * FROM TowerEntity WHERE towerNum = :towerNum and missionId =:missionId LIMIT 1")
    TowerEntity isContains(long missionId, long towerNum);

    @Delete
    void deleteMission(MissionEntity missionEntity);

    @Delete
    void deleteTower(TowerEntity towerEntity);

}
