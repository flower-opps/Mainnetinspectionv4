package com.jizhenkeji.mainnetinspection.missionrecord;

import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.dao.MissionDao;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.ButtonEvent;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareStateListener;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareType;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.RemoteHardwareController;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.text.SimpleDateFormat;
import java.util.Date;

import dji.common.flightcontroller.LocationCoordinate3D;

public class MissionRecordViewModel extends ViewModel {

    private final SimpleDateFormat df = new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)");

    public final ObservableInt observableCurrentRecordTowerNum = new ObservableInt(0);

    public final ObservableField<String> observableRecordTowerTime = new ObservableField<>("");

    private MutableLiveData<TowerEntity> mCurrentTowerEntity = new MutableLiveData<>(null);

    private MissionWithTowers mMission;

    private MissionDao mMissionDao;

    public LiveData<TowerEntity> getCurrentTower(){
        return mCurrentTowerEntity;
    }

    public void initState(MissionWithTowers missionWithTowers){
        mMission = missionWithTowers;
        mMissionDao = AppDatabase.getInstance().getMissionDao();
        initRemoteHardwareListener();
        refreshCurrentTowerRecordState();
    }

    private void initRemoteHardwareListener(){
        RemoteHardwareController.getInstance().addHardwareStateListener(this);
    }

    @HardwareStateListener(type = HardwareType.C1BUTTON)
    public void reduceRecordTowerNum(ButtonEvent event){
        switch (event){
            case DOWN:
            case LONG_DOWN:
                if(observableCurrentRecordTowerNum.get() < 1){
                    break;
                }
                observableCurrentRecordTowerNum.set(observableCurrentRecordTowerNum.get() - 1);
                break;
            case UP:
                refreshCurrentTowerRecordState();
                break;
        }
    }

    @HardwareStateListener(type = HardwareType.C2BUTTON)
    public void increaseRecordTowerNum(ButtonEvent event){
        switch (event){
            case DOWN:
            case LONG_DOWN:
                observableCurrentRecordTowerNum.set(observableCurrentRecordTowerNum.get() + 1);
                break;
            case UP:
                refreshCurrentTowerRecordState();
                break;
        }
    }

    @HardwareStateListener(type = HardwareType.PLAYBACK)
    public void recordCurrentTower(ButtonEvent event){
        if(event != ButtonEvent.UP){
            return;
        }
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            TowerEntity currentTower = mMissionDao.isContains(mMission.missionEntity.id, observableCurrentRecordTowerNum.get());
            LocationCoordinate3D location = DJIFlightControlUtil.getAircraftLocation();
            Location aircraftLocation = new Location(location.getLatitude(), location.getLongitude(), location.getAltitude());
            if(currentTower != null){
                currentTower.location = aircraftLocation;
                currentTower.createDate = new Date();
                mMissionDao.updateTower(currentTower);
            }else{
                TowerEntity newTowerEntity = new TowerEntity();
                newTowerEntity.missionId = mMission.missionEntity.id;
                newTowerEntity.towerNum = observableCurrentRecordTowerNum.get();
                newTowerEntity.location = aircraftLocation;
                mMissionDao.insertTower(newTowerEntity);
            }
            refreshCurrentTowerRecordState();
        });
    }

    private void refreshCurrentTowerRecordState(){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            TowerEntity towerEntity = mMissionDao.isContains(mMission.missionEntity.id, observableCurrentRecordTowerNum.get());
            if(towerEntity != null){
                observableRecordTowerTime.set(df.format(towerEntity.createDate));
            }else{
                observableRecordTowerTime.set("");
            }
            mCurrentTowerEntity.postValue(towerEntity);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
    }

}
