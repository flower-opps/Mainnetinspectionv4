package com.jizhenkeji.mainnetinspection.mission.followwire;

import android.os.Handler;
import android.os.Message;

import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;

import dji.common.flightcontroller.LocationCoordinate3D;


public abstract class BasePublisher {

    private Handler mTransactionHandler;

    public void setTransactionHandler(Handler handler){
        mTransactionHandler = handler;
    }

    protected void publishEvent(String event){
        Message eventMessage= mTransactionHandler.obtainMessage(FollowWireMissionExecutor.PUBLISHER_EVENT, event);
        mTransactionHandler.sendMessage(eventMessage);
    }

    private void publishMissionEvent(String event){
        Message eventMessage= mTransactionHandler.obtainMessage(FollowWireMissionExecutor.MISSION_EVENT, event);
        mTransactionHandler.sendMessage(eventMessage);
    }

    protected float getAircraftHeading(){
        float heading = DJIFlightControlUtil.getAircraftHeading();
        return Float.isNaN(heading) ? 0 : heading;
    }

    protected Location getAircraftLocation(){
        LocationCoordinate3D locationCoordinate3D = DJIFlightControlUtil.getAircraftLocation();
        return new Location(locationCoordinate3D.getLatitude(), locationCoordinate3D.getLongitude(), locationCoordinate3D.getAltitude());
    }

    protected void finish(){
        publishMissionEvent(Event.ON_MISSION_FINISH);
    }

    protected JZIError onLoad(){ return null;}

    protected void onStart(){}

    protected void onResume(){}

    protected void onRunning(){}

    protected void onPause(){}

    protected void onStop(){}

    public static class Event {

        public static final String ON_MISSION_FINISH = "BasePublisher.ON_MISSION_FINISH";

        public static final String ON_MISSION_CLEAR = "BasePublisher.ON_MISSION_CLEAR";

    }

}
