package com.jizhenkeji.mainnetinspection.missionrecord;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.ActivityMissionRecordBinding;
import com.jizhenkeji.mainnetinspection.dialog.DJIMapDialogFragment;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import dji.ux.widget.FPVOverlayWidget;

public class MissionRecordActivity extends MAppCompatActivity {

    public static final String KEY_MISSION_ENTITY = "KEY_MISSION_ENTITY";

    private ActivityMissionRecordBinding mBinding;

    private MissionRecordViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_mission_record);

        mViewModel = new ViewModelProvider(this).get(MissionRecordViewModel.class);
        mViewModel.initState((MissionWithTowers) getIntent().getSerializableExtra(KEY_MISSION_ENTITY));
        mViewModel.getCurrentTower().observe(this, (TowerEntity towerEntity) -> {
            mBinding.recordMessage.setTextColor(GlobalUtils.getColor(towerEntity == null ? R.color.white : R.color.rallyGreen));
        });

        mBinding.map.setOnClickListener(view -> {
            DJIMapDialogFragment dialogFragment = new DJIMapDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "DJIMapDialogFragment");
        });

        /* 设置屏幕网格 */
        mBinding.fpvOverlayWidget.setGridOverlayEnabled(true);
        mBinding.fpvOverlayWidget.setCurrentGridOverlayType(FPVOverlayWidget.GridOverlayType.PARALLEL_DIAGONAL);

        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);
    }

}