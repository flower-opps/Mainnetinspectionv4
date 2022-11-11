package com.jizhenkeji.mainnetinspection.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.databinding.DataItemBinding;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;

import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {

    private DataClickCallback mCallback;

    private List<DataWithMetadata> mDataWithMetadata;

    public DataAdapter(DataClickCallback callback){
        mCallback = callback;
    }

    public void setDataWithPhotos(List<DataWithMetadata> dataWithMetadata){
        mDataWithMetadata = dataWithMetadata;
        notifyDataSetChanged();
    }

    @Override
    public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DataItemBinding binding = DataItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.setCallback(mCallback);
        return new DataViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        DataWithMetadata dataWithMetadata = mDataWithMetadata.get(position);
        holder.binding.setDataWithMetadata(dataWithMetadata);
        holder.binding.setCreateDate(dataWithMetadata.dataEntity.createDate);
        holder.binding.setInspectionMode(dataWithMetadata.treePhotoEntities.size() > 0 ? InspectionMode.TREE_MODE : InspectionMode.WIRE_MODE);
    }

    @Override
    public int getItemCount() {
        return mDataWithMetadata != null ? mDataWithMetadata.size() : 0;
    }

    public static class DataViewHolder extends RecyclerView.ViewHolder {

        private final DataItemBinding binding;

        public DataViewHolder(DataItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface DataClickCallback{

        void onDownInspectionPhoto(DataWithMetadata dataWithMetadata);

        void onPreviewPhoto(DataWithMetadata dataWithMetadata);

        void onBuilderReport(DataWithMetadata dataWithMetadata);

        void onDeleteData(DataWithMetadata dataWithMetadata);

    }

}
