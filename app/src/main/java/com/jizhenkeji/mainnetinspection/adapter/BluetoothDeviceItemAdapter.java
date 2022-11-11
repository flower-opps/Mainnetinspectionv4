package com.jizhenkeji.mainnetinspection.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.R;

import java.util.ArrayList;

public class BluetoothDeviceItemAdapter extends RecyclerView.Adapter<BluetoothDeviceItemAdapter.BluetoothDeviceItemHolder> {

    public class BluetoothDeviceItemHolder extends RecyclerView.ViewHolder{

        private View rootView;

        public BluetoothDeviceItemHolder(View itemView) {
            super(itemView);
            rootView = itemView;
        }

    }

    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    /**
     * 连接蓝牙设备至列表
     * @param device
     */
    public void addBluetoothDevice(BluetoothDevice device){
        devices.add(device);
        notifyDataSetChanged();
    }

    @Override
    public BluetoothDeviceItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_item, parent, false);
        BluetoothDeviceItemHolder holder = new BluetoothDeviceItemHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(BluetoothDeviceItemHolder holder, int position) {
        View rootView = holder.rootView;
        BluetoothDevice device = devices.get(position);
        String name = device.getName();
        String address = device.getAddress();
        /* 设置蓝牙设备名称和地址 */
        TextView nameText = rootView.findViewById(R.id.name);
        nameText.setText(name);
        TextView addressText = rootView.findViewById(R.id.address);
        addressText.setText(address);
        /* 绑定点击事件 */
        rootView.setOnClickListener((View view) -> {
            if(onItemClickListener != null){
                onItemClickListener.onClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    private OnItemClickListener onItemClickListener;

    /**
     * 设置项目点击回调
     * @param onItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * 蓝牙设备item点击监听回调
     */
    public interface OnItemClickListener{

        void onClick(BluetoothDevice device);

    }

}
