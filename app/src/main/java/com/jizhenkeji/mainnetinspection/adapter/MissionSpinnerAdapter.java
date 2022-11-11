package com.jizhenkeji.mainnetinspection.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;

import java.util.List;

public class MissionSpinnerAdapter extends ArrayAdapter<String> {

    private List<MissionWithTowers> mMissionWithTowers;

    public MissionSpinnerAdapter(Context context) {
        super(context, R.layout.mission_name_item, R.id.lineName);
    }

    public void setMissionWithTowers(List<MissionWithTowers> missionWithTowers){
        mMissionWithTowers = missionWithTowers;
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return mMissionWithTowers.get(position).missionEntity.name;
    }

    @Override
    public int getCount() {
        return mMissionWithTowers != null ? mMissionWithTowers.size() : 0;
    }

}
