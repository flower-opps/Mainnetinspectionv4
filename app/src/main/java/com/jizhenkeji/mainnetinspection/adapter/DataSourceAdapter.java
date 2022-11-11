package com.jizhenkeji.mainnetinspection.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.databinding.DataSourceItemBinding;
import com.jizhenkeji.mainnetinspection.databinding.MissionItemBinding;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;

import java.util.List;


public class DataSourceAdapter extends RecyclerView.Adapter<DataSourceAdapter.DataSourceHolder> {

    private List<Device> mDevices;

    public DataSourceAdapter(DataSourceClickCallback callback){
        mDataSourceClickCallback = callback;
    }

    public void setDevices(List<Device> devices){
        mDevices = devices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DataSourceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DataSourceItemBinding binding = DataSourceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DataSourceHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DataSourceHolder holder, int position) {
        Device device = mDevices.get(position);
        holder.binding.setName(device.deviceName);
        holder.binding.setSpace(device.deviceSpace);
        holder.binding.setCallback(mDataSourceClickCallback);
    }

    @Override
    public int getItemCount() {
        return mDevices != null ? mDevices.size() : 0;
    }

    public static class DataSourceHolder extends RecyclerView.ViewHolder {

        private final DataSourceItemBinding binding;

        public DataSourceHolder(DataSourceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class Device {

        public Device(String deviceName, String deviceSpace){
            this.deviceName = deviceName;
            this.deviceSpace = deviceSpace;
        }

        public String deviceName;

        public String deviceSpace;

    }

    private DataSourceClickCallback mDataSourceClickCallback;

    public interface DataSourceClickCallback {
        void onClick(String devicePath);
    }

}
