package com.jizhenkeji.mainnetinspection.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AircraftStateMonitorService extends Service {

    private final int EVENT_CONNECT_SERVICE = 1;

    private final int EVENT_PUBLISH_STATE = 2;

    private MonitorBinder mBinder = new MonitorBinder();

    private HandlerThread mTransactionThread;

    private Handler mTransactionHandler;

    private Handler.Callback mTransactionCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case EVENT_CONNECT_SERVICE:
                    // TODO 连接至服务器
                    break;
                case EVENT_PUBLISH_STATE:
                    // TODO 获取无人机状态并发布
                    Log.d("Qimi", "发布无人机信息");
                    sendPublishStateEvent(100);
                    break;
                default:
                    return false;
            }
            return true;
        }
    };

    public void startPublishState(){
        sendPublishStateEvent(0);
    }

    public void stopPublishState(){
        mTransactionHandler.removeMessages(EVENT_PUBLISH_STATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void sendPublishStateEvent(long delay){
        mTransactionHandler.sendEmptyMessageDelayed(EVENT_PUBLISH_STATE, delay);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTransactionThread = new HandlerThread("TransactionThread");
        mTransactionThread.start();
        mTransactionHandler = new Handler(mTransactionThread.getLooper(), mTransactionCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTransactionThread.quitSafely();
    }

    public class MonitorBinder extends Binder {

        public AircraftStateMonitorService getService(){
            return AircraftStateMonitorService.this;
        }

    }

}
