package com.jizhenkeji.mainnetinspection.mission.missionset;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MissionSet implements MissionSetDispatcher {

    private MissionSetState mState = MissionSetState.READY_TO_EXECUTE;

    public ExecutorService mEventExecutor = Executors.newSingleThreadExecutor();

    private LinkedList<ProgressMission> mProgressMissionList = new LinkedList<>();

    private ProgressMission mCurrentMission;

    public synchronized void addMission(ProgressMission mission){
        mProgressMissionList.add(mission);
    }

    public synchronized void startMissionSet(){
        if(mState != MissionSetState.READY_TO_EXECUTE){
            return;
        }
        mState = MissionSetState.EXECUTING;
        mCurrentMission = mProgressMissionList.poll();
        if(mCurrentMission == null){
            mState = MissionSetState.FINISHED;
            return;
        }
        mCurrentMission.setMissionSetDispatcher(this);
        if(mListener != null) mListener.onMissionSetStart();
        dispatchEvent(mCurrentMission::onStart);
        dispatchEvent(mCurrentMission::onResume);
    }

    public synchronized void pauseMissionSet(){
        if(mState != MissionSetState.EXECUTING){
            return;
        }
        dispatchEvent(mCurrentMission::onPause);
        mState = MissionSetState.PAUSED;
    }

    public synchronized void interruptCurrentMission(){
        if(mState != MissionSetState.EXECUTING && mState != MissionSetState.PAUSED){
            return;
        }
        mCurrentMission.finish();
    }

    public synchronized void resumeMissionSet(){
        if(mState != MissionSetState.PAUSED){
            return;
        }
        dispatchEvent(mCurrentMission::onResume);
        mState = MissionSetState.EXECUTING;
    }

    public synchronized void stopMissionSet(){
        if(mState != MissionSetState.EXECUTING && mState != MissionSetState.PAUSED){
            return;
        }
        if(mState == MissionSetState.EXECUTING){
            dispatchEvent(mCurrentMission::onPause);
        }
        dispatchEvent(mCurrentMission::onStop);
        mEventExecutor.shutdown();
        mState = MissionSetState.FINISHED;
        onMissionSetStop();
    }

    private void dispatchEvent(Runnable runnable) {
        mEventExecutor.execute(runnable);
    }

    @Override
    public void finish() {
        if(mState != MissionSetState.EXECUTING){
            return;
        }
        dispatchEvent(mCurrentMission::onPause);
        dispatchEvent(mCurrentMission::onStop);
        ProgressMission nextMission = mProgressMissionList.poll();
        if(nextMission == null){
            mEventExecutor.shutdown();
            mState = MissionSetState.FINISHED;
            onMissionSetStop();
            return;
        }
        mCurrentMission = nextMission;
        mCurrentMission.setMissionSetDispatcher(this);
        dispatchEvent(mCurrentMission::onStart);
        dispatchEvent(mCurrentMission::onResume);
    }

    private void onMissionSetStop(){
        if(mListener != null) mListener.onMissionSetStop();
    }

    public enum MissionSetState {

        READY_TO_EXECUTE,

        EXECUTING,

        PAUSED,

        FINISHED;

    }

    public interface OnMissionSetStateListener {

        void onMissionSetStart();

        void onMissionSetStop();

    }

    private OnMissionSetStateListener mListener;

    public void setOnMissionSetStateListener(OnMissionSetStateListener listener){
        mListener = listener;
    }

}
