package com.jizhenkeji.mainnetinspection.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.adapter.DataSourceAdapter;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;
import com.jizhenkeji.mainnetinspection.databinding.DialogDataSourceConfigureBinding;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.StorageState;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;

/**
 * 巡检数据源选择配置对话框
 */
public class DataSourceConfigureDialogFragment extends DialogFragment {

    private final int DIALOG_WIDTH = 400;

    private final int DIALOG_HEIGHT = 300;

    private final DecimalFormat df = new DecimalFormat("#.##");

    private DialogDataSourceConfigureBinding mBinding;

    private DataSourceAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();
        Window dialogWindow = getDialog().getWindow();
        /* 固定窗体大小 */
        dialogWindow.setLayout(GlobalUtils.dpToPx(DIALOG_WIDTH), GlobalUtils.dpToPx(DIALOG_HEIGHT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogDataSourceConfigureBinding.inflate(inflater, container, false);

        mAdapter = new DataSourceAdapter(mUsbDeviceClickCallback);
        mBinding.sourceList.setAdapter(mAdapter);
        mBinding.sourceList.setLayoutManager(new LinearLayoutManager(getContext()));

        initMemoryState();
        // initUsbDeviceList();

        return mBinding.getRoot();
    }

    /**
     * 初始化无人机存储状态
     */
    private void initMemoryState(){
        mBinding.setMemoryName(GlobalUtils.getString(R.string.unknown_storage_state));
        mBinding.setMemorySpace(GlobalUtils.getString(R.string.unknown_storage_state));
        if(MediaDataController.getInstance().isSupportWirelessDownload()){
            mBinding.include.dataSourceCard.setAlpha(1f);
            mBinding.include.dataSourceCard.setEnabled(true);
            mBinding.setCallback(mMemoryClickCallback);
        }else{
            mBinding.include.dataSourceCard.setAlpha(0.5f);
            mBinding.include.dataSourceCard.setEnabled(false);
        }
        /* 获取存储信息 */
        Camera camera = CameraController.getInstance().getCamera();
        camera.setStorageStateCallBack((StorageState storageState) -> {
            switch (storageState.getStorageLocation()){
                case INTERNAL_STORAGE:
                    mBinding.setMemoryName(GlobalUtils.getString(R.string.internal_storage));
                    break;
                case SDCARD:
                    mBinding.setMemoryName(GlobalUtils.getString(R.string.external_storage));
                    break;
            }
            String remainingSpace = df.format(storageState.getRemainingSpaceInMB() / 1024f) + "/"
                    + df.format(storageState.getTotalSpaceInMB() / 1024f) + " GB";
            mBinding.setMemorySpace(remainingSpace);
        });
    }

    /**
     * 初始化连接的USB设备列表
     */
    private void initUsbDeviceList(){
        MediaDataController.getInstance().setOnUsbDeviceChangeCallback((String[] strings) -> {
            List<DataSourceAdapter.Device> devices = new ArrayList<>();
            for(String deviceName:strings){
                File sdCardFilePath = new File(deviceName);
                if(sdCardFilePath.exists()){
                    String spaceDescription = df.format(sdCardFilePath.getFreeSpace() / 1024f / 1024f / 1024f) + "/" +
                            df.format(sdCardFilePath.getTotalSpace() / 1024f / 1024f / 1024f) + " GB";
                    devices.add(new DataSourceAdapter.Device(deviceName, spaceDescription));
                }
            }
            mAdapter.setDevices(devices);
        });
    }

    private DataSourceAdapter.DataSourceClickCallback mUsbDeviceClickCallback = new DataSourceAdapter.DataSourceClickCallback() {
        @Override
        public void onClick(String devicePath) {
            MediaDataController.getInstance().initUsbDeviceStrategy(devicePath);
            if(mCallback != null) mCallback.onConfigured();
            dismiss();
        }
    };

    private DataSourceAdapter.DataSourceClickCallback mMemoryClickCallback = new DataSourceAdapter.DataSourceClickCallback() {
        @Override
        public void onClick(String devicePath) {
            MediaDataController.getInstance().initWirelessStrategy();
            if(mCallback != null) mCallback.onConfigured();
            dismiss();
        }
    };

    private OnConfigureCallback mCallback;

    public void setOnConfigureCallback(OnConfigureCallback callback){
        mCallback = callback;
    }

    public interface OnConfigureCallback {

        void onConfigured();

    }

}
