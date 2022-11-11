package com.jizhenkeji.mainnetinspection.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.databinding.DataItemBinding;
import com.jizhenkeji.mainnetinspection.databinding.DataNewitemBinding;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;

import java.util.ArrayList;
import java.util.List;

public class PhaseDataAdapter extends RecyclerView.Adapter<PhaseDataAdapter.DataViewHolder> {

    private DataClickCallback mCallback;

    private List<DataWithMetadata> mDataWithMetadata;

    public ArrayList<DataWithMetadata>  mCheckDataWithMetadata= new ArrayList<>();

    public PhaseDataAdapter(DataClickCallback callback){
        mCallback = callback;
    }

    public void setDataWithPhotos(List<DataWithMetadata> dataWithMetadata){
        mDataWithMetadata = dataWithMetadata;
        notifyDataSetChanged();
    }

    @Override
    public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DataNewitemBinding binding = DataNewitemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
//        binding.setMCheckDataWithMetadata(mCheckDataWithMetadata);
        binding.setCallback(mCallback);
        return new DataViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        DataWithMetadata dataWithMetadata = mDataWithMetadata.get(position);
        holder.binding.setDataWithMetadata(dataWithMetadata);
        holder.binding.setCreateDate(dataWithMetadata.dataEntity.createDate);
        try {
            if(mCheckDataWithMetadata.contains(dataWithMetadata)) {
                holder.binding.cbox.setChecked(true);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        holder.binding.cbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ArrayList<DataWithMetadata> mDataWithMetadata=mCheckDataWithMetadata;
                if(b){
                    mDataWithMetadata.add(dataWithMetadata);
                }else{
                    for (int i = 0; i < mDataWithMetadata.size(); i++) {
                        DataWithMetadata item = mDataWithMetadata.get(i);
                        if (item==dataWithMetadata) {
                            mDataWithMetadata.remove(i);
                        }
                    }
                }
                mCheckDataWithMetadata=mDataWithMetadata;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataWithMetadata != null ? mDataWithMetadata.size() : 0;
    }

    public static class DataViewHolder extends RecyclerView.ViewHolder {

        private final DataNewitemBinding binding;

        public DataViewHolder(DataNewitemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface DataClickCallback{

    }

}
