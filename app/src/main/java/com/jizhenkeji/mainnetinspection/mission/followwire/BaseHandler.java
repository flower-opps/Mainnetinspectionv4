package com.jizhenkeji.mainnetinspection.mission.followwire;

import android.os.Handler;
import android.os.Message;

public abstract class BaseHandler {

    private static Handler mTransactionHandler;

    public void setTransactionHandler(Handler handler){
        mTransactionHandler = handler;
    }

    public static void publishEvent(String event){
        Message eventMessage= mTransactionHandler.obtainMessage(FollowWireMissionExecutor.PUBLISHER_EVENT, event);
        mTransactionHandler.sendMessage(eventMessage);
    }

}
