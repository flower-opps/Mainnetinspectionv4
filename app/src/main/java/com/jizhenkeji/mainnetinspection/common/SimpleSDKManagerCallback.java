package com.jizhenkeji.mainnetinspection.common;

import dji.common.error.DJIError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public abstract class SimpleSDKManagerCallback implements DJISDKManager.SDKManagerCallback {

    @Override
    public abstract void onRegister(DJIError djiError);

    @Override
    public void onProductDisconnect() {

    }

    @Override
    public abstract void onProductConnect(BaseProduct baseProduct);

    @Override
    public abstract void onProductChanged(BaseProduct baseProduct);

    @Override
    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent baseComponent, BaseComponent baseComponent1) {}

    @Override
    public abstract void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i);

    @Override
    public void onDatabaseDownloadProgress(long l, long l1) {}

}
