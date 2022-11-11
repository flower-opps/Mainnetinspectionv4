package com.jizhenkeji.mainnetinspection.dialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.adapter.BluetoothDeviceItemAdapter;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 蓝牙连接对话框
 */
public class BluetoothConnectDialogFragment extends DialogFragment {

    private final String TAG = "BluetoothConnect";

    /**
     * 雷达启动Intent标志位
     */
    private final int BLUETOOTH_OPEN_CODE = 0x01;

    /**
     * 蓝牙设备列表对象
     */
    private RecyclerView bluetoothListView;

    /**
     * 跳过视图对象
     */
    private TextView mSkipTextView;

    /**
     * 蓝牙设备列表适配器
     */
    private BluetoothDeviceItemAdapter mAdapter;

    /**
     * 保存已搜索到的蓝牙设备
     */
    private Set<BluetoothDevice> searchedBluetooths = new HashSet<>();

    /* 蓝牙设备搜索结果广播接收 */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName() != null && !searchedBluetooths.contains(device)){
                    searchedBluetooths.add(device);
                    mAdapter.addBluetoothDevice(device);    // 添加设备至列表
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        /* 设置窗体大小 */
        int width = GlobalUtils.dpToPx(500);
        int height = GlobalUtils.dpToPx(350);
        getDialog().getWindow().setLayout(width, height);
        setCancelable(false);
        /* 判断是否开启蓝牙 */
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            /* 未开启则进入系统界面开启雷达 */
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_OPEN_CODE);
        }else{
            /* 已开启，则开始监听蓝牙广播 */
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(mBluetoothReceiver, intentFilter);
            boolean isStart = bluetoothAdapter.startDiscovery();
            Log.d(TAG, "开始搜索蓝牙：" + isStart);
            if(!isStart){
                Toast.makeText(getActivity(), "蓝牙开启搜索失败，检查权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_bluetooth_connect, container, false);
        bluetoothListView = rootView.findViewById(R.id.bluetoothListView);
        mSkipTextView = rootView.findViewById(R.id.skipButton);
        mAdapter = new BluetoothDeviceItemAdapter();
        mAdapter.setOnItemClickListener((BluetoothDevice device) -> {
            /* 连接蓝牙设备 */
            if(onConnectCallback != null){
                onConnectCallback.onConnect(device);
            }
            dismiss();
        });
        mSkipTextView.setOnClickListener((View view) -> {
            /* 连接蓝牙设备 */
            if(onConnectCallback != null){
                onConnectCallback.onConnect(null);
            }
            dismiss();
        });
        bluetoothListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        bluetoothListView.setAdapter(mAdapter);
        ImageView closeButton = rootView.findViewById(R.id.close_button);
        closeButton.setOnClickListener((View view) -> {
            if(onConnectCallback != null){
                onConnectCallback.onCancel();
            }
            dismiss();
        });
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.cancelDiscovery();
        getActivity().unregisterReceiver(mBluetoothReceiver);
    }

    private OnConnectCallback onConnectCallback;

    public void setOnConnectCallback(OnConnectCallback onConnectCallback){
        this.onConnectCallback = onConnectCallback;
    }

    public interface OnConnectCallback{

        void onConnect(BluetoothDevice device);

        void onCancel();

    }

}
