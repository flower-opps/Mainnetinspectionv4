package com.jizhenkeji.mainnetinspection.initsdk;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.common.SimpleSDKManagerCallback;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.controller.gimbalcontroller.GimbalController;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.RemoteHardwareController;
import com.jizhenkeji.mainnetinspection.controller.virtualstickcontroller.VirtualStickController;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class InitViewModel extends AndroidViewModel {

    public final ObservableField<String> observableRegisterMessage = new ObservableField<>(GlobalUtils.getString(R.string.init_sdk));

    public final ObservableInt observableRegisterProgress = new ObservableInt(GlobalUtils.getInteger(R.integer.init_sdk));

    private MutableLiveData<Boolean> mProductConnectSuccess = new MutableLiveData<>();

    private final Context mContext;

    public InitViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
    }

    public LiveData<Boolean> getProductConnected(){
        return mProductConnectSuccess;
    }

    public static BaseProduct baseProduct;
    public void registerSDK(){
        DJISDKManager.getInstance().registerApp(mContext, new SimpleSDKManagerCallback() {
            @Override
            public void onRegister(DJIError djiError) {
                if(djiError != DJISDKError.REGISTRATION_SUCCESS){
                    observableRegisterMessage.set(GlobalUtils.getString(R.string.init_sdk_failure));
                    observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_sdk_failure));
                    return;
                }
                observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_sdk_success));
                observableRegisterMessage.set(GlobalUtils.getString(R.string.init_sdk_success));
                DJISDKManager.getInstance().startConnectionToProduct();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                InitViewModel.baseProduct = baseProduct;

                if(baseProduct.isConnected()){
                    try {
                        onConnectProductSuccess();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_wait_connect));
                    observableRegisterMessage.set(GlobalUtils.getString(R.string.init_wait_connect));
                }
            }

            @Override
            public void onProductChanged(BaseProduct baseProduct) {
                if(baseProduct.isConnected()){
                    try {
                        onConnectProductSuccess();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
                observableRegisterMessage.set(GlobalUtils.getString(R.string.init_in_progress) + djisdkInitEvent.getInitializationState().name());
                observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_in_progress));
            }
        });
    }

    /**
     * 连接无人机标志位
     */
    private volatile boolean isConnectProduct = false;

    private synchronized void onConnectProductSuccess() throws InterruptedException {
        /* 防止本方法被多次调用 */
        if(isConnectProduct){
            return;
        }
        isConnectProduct = true;
        observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_camera_controller));
        observableRegisterMessage.set(GlobalUtils.getString(R.string.init_camera_controller));
        JZIError cameraControllerError = CameraController.getInstance().init();
        if(cameraControllerError != null){
            observableRegisterMessage.set(GlobalUtils.getString(R.string.init_camera_controller_error));
            wait(5000);
            //return;
        }
        observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_remote_hareware_controller));
        observableRegisterMessage.set(GlobalUtils.getString(R.string.init_remote_hareware_controller));
        JZIError remoteHardwareControllerError = RemoteHardwareController.getInstance().init();
        if(remoteHardwareControllerError != null){
            observableRegisterMessage.set(GlobalUtils.getString(R.string.init_remote_hareware_controller_error));
            return;
        }
        observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_virtual_stick_controller));
        observableRegisterMessage.set(GlobalUtils.getString(R.string.init_virtual_stick_controller));
        JZIError virtualStickControllerError = VirtualStickController.getInstance().init();
        if(virtualStickControllerError != null){
            observableRegisterMessage.set(GlobalUtils.getString(R.string.init_virtual_stick_controller_error));
            return;
        }
        observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_media_data_controller));
        observableRegisterMessage.set(GlobalUtils.getString(R.string.init_media_data_controller));
        JZIError mediaDataControllerError = MediaDataController.getInstance().init();
        if(mediaDataControllerError != null){
            observableRegisterMessage.set(GlobalUtils.getString(R.string.init_media_data_controller_error));
            return;
        }
        observableRegisterProgress.set(GlobalUtils.getInteger(R.integer.init_gimbal_controller));
        observableRegisterMessage.set(GlobalUtils.getString(R.string.init_gimbal_controller));
        JZIError gimbalControllerError = GimbalController.getInstance().init();
        if(gimbalControllerError != null){
            observableRegisterMessage.set(GlobalUtils.getString(R.string.init_gimbal_controller_error));
            return;
        }


        mProductConnectSuccess.postValue(true);
    }

}
