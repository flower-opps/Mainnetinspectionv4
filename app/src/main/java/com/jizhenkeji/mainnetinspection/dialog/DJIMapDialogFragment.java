package com.jizhenkeji.mainnetinspection.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJICircleOptions;
import com.dji.mapkit.core.models.annotations.DJIPolylineOptions;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogDjiMapBinding;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import java.util.HashMap;
import java.util.List;

public class DJIMapDialogFragment extends DialogFragment {

    private DialogDjiMapBinding mBinding;

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8f);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8f);
        window.setLayout(width, height);
        window.setDimAmount(0);
        window.setBackgroundDrawable(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBinding.mapWidget.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogDjiMapBinding.inflate(inflater, container, false);

        mBinding.mapWidget.initAMap(null);
        mBinding.mapWidget.onCreate(savedInstanceState);

        return mBinding.getRoot();
    }

    /**
     * 显示巡检台账数据，在地图上描绘两个杆塔点然后连线
     * @param missionWithTowers
     */
    public void showInspectionMission(MissionWithTowers missionWithTowers){
        HashMap<Long, TowerEntity> towerEntityHashMap = new HashMap<>();
        List<TowerEntity> towerEntities = missionWithTowers.towerEntities;
        DJIMap djiMap = mBinding.mapWidget.getMap();
        /* 如果已加载的点中包含了相邻的点，则连接两个点 */
        for(TowerEntity towerEntity : towerEntities){
            TowerEntity preTowerEntity = towerEntityHashMap.get(towerEntity.towerNum--);
            if(preTowerEntity != null){
                DJIPolylineOptions polylineOptions = new DJIPolylineOptions()
                        .add(new DJILatLng(preTowerEntity.location.latitude, preTowerEntity.location.longitude))
                        .add(new DJILatLng(towerEntity.location.latitude, towerEntity.location.longitude))
                        .width(10)
                        .color(GlobalUtils.getColor(R.color.rallyGreen));
                djiMap.addPolyline(polylineOptions);
            }
            TowerEntity nextTowerEntity = towerEntityHashMap.get(towerEntity.towerNum++);
            if(nextTowerEntity != null){
                DJIPolylineOptions polylineOptions = new DJIPolylineOptions()
                        .add(new DJILatLng(nextTowerEntity.location.latitude, nextTowerEntity.location.longitude))
                        .add(new DJILatLng(towerEntity.location.latitude, towerEntity.location.longitude))
                        .width(10)
                        .color(GlobalUtils.getColor(R.color.rallyGreen));
                djiMap.addPolyline(polylineOptions);
            }
            towerEntityHashMap.put(towerEntity.towerNum, towerEntity);
        }
        /* 描绘圆点 */
        for(TowerEntity towerEntity : towerEntities){
            Location towerLocation = towerEntity.location;
            DJICircleOptions circleOptions = new DJICircleOptions()
                    .center(new DJILatLng(towerLocation.latitude, towerLocation.longitude))
                    .radius(3)
                    .strokeWidth(2)
                    .fillColor(GlobalUtils.getColor(R.color.rallyOrange))
                    .strokeColor(GlobalUtils.getColor(R.color.rallyOrange));
            djiMap.addSingleCircle(circleOptions);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBinding.mapWidget.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBinding.mapWidget.onDestroy();
    }
}
