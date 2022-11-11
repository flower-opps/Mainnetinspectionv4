package com.jizhenkeji.mainnetinspection.missionmanage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.MissionEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.util.List;

public class MissionManageViewModel extends ViewModel {

    private MutableLiveData<String> mToastMessage = new MutableLiveData<>();

    public LiveData<String> getToastMessage(){
        return mToastMessage;
    }

    public LiveData<List<MissionWithTowers>> getMissions(){
        return AppDatabase.getInstance().getMissionDao().getAllMissions();
    }

    public void onCreateMission(String lineName, String voltageLevel, String manageClassName){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            MissionEntity missionEntity = new MissionEntity();
            missionEntity.name = lineName;
            missionEntity.voltageLevel = voltageLevel;
            missionEntity.manageClassName = manageClassName;
            AppDatabase.getInstance().getMissionDao().insertMission(missionEntity);
            mToastMessage.postValue(GlobalUtils.getString(R.string.mission_create_success));
        });
    }

}
