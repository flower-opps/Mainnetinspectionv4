package com.jizhenkeji.mainnetinspection.mission.followwirev2;

import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FollowWireV2MissionExecutor {

    public static FollowWireV2MissionExecutor getInstance(){
        return Instance.INSTANCE;
    }

    private FollowWireV2MissionExecutor(){}

    private static class Instance {
        private static final FollowWireV2MissionExecutor INSTANCE = new FollowWireV2MissionExecutor();
    }

    private State mState = State.READY_TO_LOAD;

    private MissionExecuteThread mMissionExecuteThread;

    private FollowWireV2Mission mMission;

    public FollowWireV2MissionError loadMission(FollowWireV2Mission mission){
        if(mState != State.READY_TO_LOAD){
            return FollowWireV2MissionError.EXECUTOR_STATE_ERROR;
        }
        if(mission == null || mission.getRadarManager() == null || mission.getFlightDirection() == FlightDirection.UNKNOWN){
            return FollowWireV2MissionError.MISSION_PARAMETER_ERROR;
        }
        mMission = mission;
        mMissionExecuteThread = new MissionExecuteThread();
        mState = State.READY_TO_START;
        return null;
    }

    public FollowWireV2MissionError startMission(){
        if(mState != State.READY_TO_START){
            return FollowWireV2MissionError.EXECUTOR_STATE_ERROR;
        }
        mState = State.INSPECTING;
        mMissionExecuteThread = new MissionExecuteThread();
        mMissionExecuteThread.setDaemon(true);
        mMissionExecuteThread.start();
        return null;
    }

    public FollowWireV2MissionError resumeMission(){
        if(mState != State.PAUSED){
            return FollowWireV2MissionError.EXECUTOR_STATE_ERROR;
        }
        mMissionExecuteThread.mThreadDispatchLock.lock();
        mMissionExecuteThread.mPauseStateCondition.signalAll();
        mMissionExecuteThread.mThreadDispatchLock.unlock();
        mState = State.INSPECTING;
        return null;
    }

    public FollowWireV2MissionError pauseMission(){
        if(mState != State.INSPECTING){
            return FollowWireV2MissionError.EXECUTOR_STATE_ERROR;
        }
        try{
            mMissionExecuteThread.mThreadDispatchLock.lock();
            if(!mMissionExecuteThread.mPauseStateCondition.await(1000, TimeUnit.MILLISECONDS)){
                return FollowWireV2MissionError.NO_RESPONSE;
            }
            mMissionExecuteThread.mThreadDispatchLock.unlock();
            mState = State.PAUSED;
        } catch (InterruptedException e) {
            mMissionExecuteThread.mThreadDispatchLock.unlock();
        }
        return null;
    }

    public FollowWireV2MissionError stopMission(){
        if(mState != State.INSPECTING && mState != State.PAUSED){
            return FollowWireV2MissionError.EXECUTOR_STATE_ERROR;
        }
        mMissionExecuteThread.interrupt();
        mState = State.READY_TO_LOAD;
        return null;
    }

    private class MissionExecuteThread extends Thread {

        private ReentrantLock mThreadDispatchLock = new ReentrantLock();

        private Condition mPauseStateCondition = mThreadDispatchLock.newCondition();
        @Override
        public void run() {
            /* 执行任务onStart事务 */
            mMission.onStart();
            /* 执行任务onResume事务 */
            mMission.onResume();
            /* 循环执行咬线动作 */
            try {
                while(!interrupted()){
                    mMission.onInspecting();
                    mThreadDispatchLock.lock();
                    if(mThreadDispatchLock.hasWaiters(mPauseStateCondition)){
                        /* 进入暂停状态 */
                        mState = FollowWireV2MissionExecutor.State.PAUSED;
                        mPauseStateCondition.signal();
                        mMission.onPause();
                        /* 恢复运行状态 */
                        mPauseStateCondition.await();
                        mState = FollowWireV2MissionExecutor.State.INSPECTING;
                        mMission.onResume();
                    }
                    mThreadDispatchLock.unlock();
                }
                /* 执行onPause事务 */
                mMission.onPause();
            } catch (InterruptedException e) {
                mThreadDispatchLock.unlock();
            }
            /* 执行onStop事务 */
            mMission.onStop();
        }

    }

    private enum State {

        READY_TO_LOAD,

        READY_TO_START,

        INSPECTING,

        PAUSED;

    }

}
