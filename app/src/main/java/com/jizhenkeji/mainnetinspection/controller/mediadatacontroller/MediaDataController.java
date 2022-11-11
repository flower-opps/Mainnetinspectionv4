package com.jizhenkeji.mainnetinspection.controller.mediadatacontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy.MediaDataDownStrategy;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy.UsbDeviceStrategy;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.strategy.WirelessStrategy;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import dji.common.product.Model;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.sdk.media.MediaFile;

public class MediaDataController extends ComponentController implements MediaDataDownStrategy {

    public static MediaDataController getInstance() {
        return Controller.INSTANCE;
    }

    private static class Controller {
        private static final MediaDataController INSTANCE = new MediaDataController();
    }

    private MediaDataController() {}

    private Set<String> mConnectedUsbDevicePathSet = new HashSet<>();

    private OnUsbDeviceChangeCallback mCallback;

    private MediaDataDownStrategy mMediaDataDownStrategy;

    @Override
    public JZIError init() {
        registerBroadcastReceiver();
        return null;
    }

    /**
     * 初始化无线策略
     */
    public void initWirelessStrategy(){
        releaseDownStrategy();
        mMediaDataDownStrategy = new WirelessStrategy();
    }

    /**
     * 初始化USB设备策略
     * @param devicePath
     */
    public void initUsbDeviceStrategy(String devicePath){
        releaseDownStrategy();
        mMediaDataDownStrategy = new UsbDeviceStrategy(devicePath);
    }

    @Override
    public void getThumbnailPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        if(mMediaDataDownStrategy == null){
            if(callback != null) callback.onResult(MediaDataControllerError.UNCONFIGURED_STRATEGY);
            return;
        }
        mMediaDataDownStrategy.getThumbnailPhoto(mediaFile, outputStream, callback);
    }

    @Override
    public void getPreviewPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        if(mMediaDataDownStrategy == null){
            if(callback != null) callback.onResult(MediaDataControllerError.UNCONFIGURED_STRATEGY);
            return;
        }
        mMediaDataDownStrategy.getPreviewPhoto(mediaFile, outputStream, callback);
    }

    @Override
    public void getRawPhoto(MediaFile mediaFile, OutputStream outputStream, CommonCallback<JZIError> callback) {
        if(mMediaDataDownStrategy == null){
            if(callback != null) callback.onResult(MediaDataControllerError.UNCONFIGURED_STRATEGY);
            return;
        }
        mMediaDataDownStrategy.getRawPhoto(mediaFile, outputStream, callback);
    }

    /**
     * 返回是否支持MSDK的远程媒体文件下载
     * @return
     */
    public boolean isSupportWirelessDownload(){
        ProductKey productModelKey = ProductKey.create(ProductKey.MODEL_NAME);
        Model model = (Model) KeyManager.getInstance().getValue(productModelKey);
        if(model == null){
            return false;
        }
        return true;  // TODO
    }

    /**
     * 设置Usb设备连接状态回调
     */
    public void setOnUsbDeviceChangeCallback(OnUsbDeviceChangeCallback callback){
        mCallback = callback;
        if(mCallback != null) {
            mCallback.onChange(getDevicesPath());
        }
    }

    /**
     * 获取当前已连接的Usb设备路径
     * @return
     */
    public String[] getDevicesPath(){
        return mConnectedUsbDevicePathSet.toArray(new String[mConnectedUsbDevicePathSet.size()]);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri deviceUri = intent.getData();
            switch (intent.getAction()){
                case Intent.ACTION_MEDIA_MOUNTED:
                    mConnectedUsbDevicePathSet.add(deviceUri.getPath());
                    break;
                case Intent.ACTION_MEDIA_UNMOUNTED:
                    mConnectedUsbDevicePathSet.remove(deviceUri.getPath());
                    break;
            }
            if(mCallback != null) {
                mCallback.onChange(getDevicesPath());
            }
        }
    };

    private void registerBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addDataScheme("file");
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        GlobalUtils.getApplicationContext().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver(){
        GlobalUtils.getApplicationContext().unregisterReceiver(mBroadcastReceiver);
    }

    private void releaseDownStrategy(){
        if(mMediaDataDownStrategy != null){
            mMediaDataDownStrategy.release();
        }
    }

    @Override
    public void release() {
        super.release();
        unregisterBroadcastReceiver();
        releaseDownStrategy();
        mCallback = null;
    }

    public interface OnUsbDeviceChangeCallback {

        void onChange(String[] paths);

    }

}
