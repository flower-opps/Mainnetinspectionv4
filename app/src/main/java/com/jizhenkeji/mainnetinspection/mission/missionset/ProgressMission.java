package com.jizhenkeji.mainnetinspection.mission.missionset;

public abstract class ProgressMission {

    private MissionSetDispatcher mDispatcher;

    private boolean isFinished = false;

    protected void setMissionSetDispatcher(MissionSetDispatcher dispatcher){
        mDispatcher = dispatcher;
    }

    protected synchronized void finish(){
        if(mDispatcher != null && !isFinished) {
            isFinished = true;
            mDispatcher.finish();
        }
    }

    protected void onStart(){}

    protected void onResume(){}

    protected void onPause(){}

    protected void onStop(){}

}
