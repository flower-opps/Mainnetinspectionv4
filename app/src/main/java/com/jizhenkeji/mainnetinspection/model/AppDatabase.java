package com.jizhenkeji.mainnetinspection.model;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.jizhenkeji.mainnetinspection.model.converter.DataConverters;
import com.jizhenkeji.mainnetinspection.model.converter.MissionConverters;
import com.jizhenkeji.mainnetinspection.model.dao.DataDao;
import com.jizhenkeji.mainnetinspection.model.dao.MissionDao;
import com.jizhenkeji.mainnetinspection.model.entity.DataEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TerrainEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;


@Database(entities = {
        MissionEntity.class, TowerEntity.class,
        DataEntity.class, WirePhotoEntity.class, TreePhotoEntity.class, TerrainEntity.class}, version = 1, exportSchema = false)
@TypeConverters({MissionConverters.class, DataConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    /**
     * 获取数据库对象实例
     * @return
     */
    public static AppDatabase getInstance(){
        if(INSTANCE == null){
            synchronized (AppDatabase.class){
                if(INSTANCE == null) INSTANCE = Room.databaseBuilder(GlobalUtils.getApplicationContext(), AppDatabase.class, "jzi.db").build();
            }
        }
        return INSTANCE;
    }

    public abstract MissionDao getMissionDao();

    public abstract DataDao getDataDao();

}
