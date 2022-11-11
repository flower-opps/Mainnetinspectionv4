package com.jizhenkeji.mainnetinspection.datamanage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;

import java.util.List;

public class DataManageViewModel extends ViewModel {

    public LiveData<List<DataWithMetadata>> getDatas(){
        return AppDatabase.getInstance().getDataDao().getAllDatas();
    }

    public LiveData<List<DataWithMetadata>> getForIdDatas(long id){
        return AppDatabase.getInstance().getDataDao().getForIdDatas(id);
    }

}
