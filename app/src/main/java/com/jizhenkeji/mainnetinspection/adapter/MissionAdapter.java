package com.jizhenkeji.mainnetinspection.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.databinding.MissionItemBinding;
import com.jizhenkeji.mainnetinspection.model.entity.MissionEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;

import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.ProductViewHolder> {

    private List<MissionWithTowers> mMissionList;

    public MissionAdapter(MissionClickCallback callback){
        mMissionClickCallback = callback;
    }

    public void setMissionList(List<MissionWithTowers> missionList){
        mMissionList = missionList;
        notifyDataSetChanged();
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MissionItemBinding binding = MissionItemBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.setCallback(mMissionClickCallback);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        holder.binding.setMission(mMissionList.get(position));
    }

    @Override
    public int getItemCount() {
        return mMissionList == null ? 0 : mMissionList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final MissionItemBinding binding;

        public ProductViewHolder(MissionItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private MissionClickCallback mMissionClickCallback;

    public interface MissionClickCallback {

        void onEditMission(MissionWithTowers mission);

        void onDeleteMission(MissionWithTowers mission);

        void onSearchTowerPoint(MissionWithTowers mission);

        void onExportMission(MissionWithTowers mission);

        void onKmlOut(MissionWithTowers mission);

        void onKmlIn(MissionWithTowers mission);

        void onPhaseSpacing(MissionWithTowers mission);

    }

}
