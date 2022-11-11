package com.jizhenkeji.mainnetinspection.mission.followwire;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.jizhenkeji.mainnetinspection.common.JZIError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FollowWireMissionExecutor {

    public static final int PUBLISHER_EVENT = 0x01;

    public static final int MISSION_EVENT = 0x02;

    public static FollowWireMissionExecutor getInstance(){
        return Instance.INSTANCE;
    }

    private FollowWireMissionExecutor(){}

    private static class Instance {
        private static final FollowWireMissionExecutor INSTANCE = new FollowWireMissionExecutor();
    }

    public static FollowWireMissionExecutorState mState = FollowWireMissionExecutorState.READY_TO_LOAD;

    private MissionExecuteThread mMissionExecuteThread;

    private HandlerThread mEventpublishThread;

    private Handler mEventPublishHandler;

    private FollowWireMission mFollowWireMission;

    public JZIError loadMission(FollowWireMission followWireMission){
        if(mState != FollowWireMissionExecutorState.READY_TO_LOAD){
            return FollowWireMissionError.INTERNAL_STATE_ERROR;
        }
        List<BasePublisher> publishers = followWireMission.getPublishers();
        for(BasePublisher publisher : publishers){
            JZIError jziError = publisher.onLoad();
            if(jziError != null) {
                return jziError;
            }
        }
        mFollowWireMission = followWireMission;
        /* 修改内部状态 */
        mState = FollowWireMissionExecutorState.READY_TO_START;
        return null;
    }

    public JZIError startMission(){
        if(mState != FollowWireMissionExecutorState.READY_TO_START){
            return FollowWireMissionError.INTERNAL_STATE_ERROR;
        }
        /* 开启事务发布处理线程 */
        mEventpublishThread = new HandlerThread("EventpublishThread");
        mEventpublishThread.setDaemon(true);
        mEventpublishThread.start();
        mEventPublishHandler = new Handler(mEventpublishThread.getLooper(), mEventPublishCallback);
        for(BasePublisher basePublisher : mFollowWireMission.getPublishers()){
            basePublisher.setTransactionHandler(mEventPublishHandler);
        }
        for(BaseHandler baseHandler : mFollowWireMission.getHandlers()){
            baseHandler.setTransactionHandler(mEventPublishHandler);
        }
        /* 开启任务执行线程 */
        mMissionExecuteThread = new MissionExecuteThread();
        mMissionExecuteThread.setDaemon(true);
        mMissionExecuteThread.start();
        /* 修改内部状态 */
        mState = FollowWireMissionExecutorState.EXECUTING;
        return null;
    }

    private class MissionExecuteThread extends Thread {

        private ReentrantLock mStateDispatchLock = new ReentrantLock();

        private Condition mPauseStateCondition = mStateDispatchLock.newCondition();

        @Override
        public void run() {
            /* 执行onStart事务 */
            onStart(mFollowWireMission.getPublishers());
            /* 执行onResume事务 */
            onResume(mFollowWireMission.getPublishers());
            /* 执行onRunning事务 */
            try {
                while(!Thread.interrupted()){
                    onRunning(mFollowWireMission.getPublishers());
                    mStateDispatchLock.lock();
                    if(mStateDispatchLock.hasWaiters(mPauseStateCondition)){
                        /* 进入暂停状态 */
                        mState = FollowWireMissionExecutorState.PAUSED;
                        mPauseStateCondition.signal();
                        onPause(mFollowWireMission.getPublishers());
                        /* 恢复运行状态 */
                        mPauseStateCondition.await();
                        mState = FollowWireMissionExecutorState.EXECUTING;
                        onResume(mFollowWireMission.getPublishers());
                    }
                    mStateDispatchLock.unlock();
                }
                /* 执行onPause事务 */
                onPause(mFollowWireMission.getPublishers());
            } catch (InterruptedException e) {
                mStateDispatchLock.unlock();
            }
            /* 执行onStop事务 */
            onStop(mFollowWireMission.getPublishers());
            /* 发布清除任务事务 */
            Message eventMessage= mEventPublishHandler.obtainMessage(FollowWireMissionExecutor.MISSION_EVENT, BasePublisher.Event.ON_MISSION_CLEAR);
            mEventPublishHandler.sendMessage(eventMessage);
        }

        private void onStart(List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                basePublisher.onStart();
            }
        }

        private void onResume(List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                basePublisher.onResume();
            }
        }

        private void onRunning(List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                basePublisher.onRunning();
            }
        }

        private void onPause(List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                basePublisher.onPause();
            }
        }

        private void onStop(List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                basePublisher.onStop();
            }
        }

    }

    public JZIError pauseMission(){
        if(mState != FollowWireMissionExecutorState.EXECUTING){
            return FollowWireMissionError.INTERNAL_STATE_ERROR;
        }
        try{
            mMissionExecuteThread.mStateDispatchLock.lock();
            if(!mMissionExecuteThread.mPauseStateCondition.await(1000, TimeUnit.MILLISECONDS)){
                return FollowWireMissionError.NO_RESPONSE;
            }
            mMissionExecuteThread.mStateDispatchLock.unlock();
        } catch (InterruptedException e) {
            mMissionExecuteThread.mStateDispatchLock.unlock();
        }
        return null;
    }

    public JZIError resumeMission(){
        if(mState != FollowWireMissionExecutorState.PAUSED){
            return FollowWireMissionError.INTERNAL_STATE_ERROR;
        }
        mMissionExecuteThread.mStateDispatchLock.lock();
        mMissionExecuteThread.mPauseStateCondition.signalAll();
        mMissionExecuteThread.mStateDispatchLock.unlock();
        return null;
    }

    public JZIError stopMission(){
        if(mState != FollowWireMissionExecutorState.PAUSED && mState != FollowWireMissionExecutorState.EXECUTING){
            return FollowWireMissionError.INTERNAL_STATE_ERROR;
        }
        mMissionExecuteThread.interrupt();
        mState = FollowWireMissionExecutorState.READY_TO_LOAD;
        return null;
    }

//    public void clearMission(){
//        if(mFollowWireMission == null || mState != FollowWireMissionExecutorState.READY_TO_LOAD){
//            return;
//        }
//        for(BasePublisher basePublisher : mFollowWireMission.getPublishers()){
//            basePublisher.setTransactionHandler(null);
//        }
//        for(BaseHandler baseHandler : mFollowWireMission.getHandlers()){
//            baseHandler.setTransactionHandler(null);
//        }
//        mFollowWireMission = null;
//    }

    private Handler.Callback mEventPublishCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String event = (String) msg.obj;
            if(event == null || mFollowWireMission == null) {
                return false;
            }
            switch (msg.what){
                case PUBLISHER_EVENT:
                    /* 发布事务到处理者 */
                    publishTransactionToHandler(event, msg,  mFollowWireMission.getHandlers());
                    /* 发布事务到任务发布者 */
                    publishTransactionToPublisher(event, msg, mFollowWireMission.getPublishers());
                    break;
                case MISSION_EVENT:
                    switch (event){
                        case BasePublisher.Event.ON_MISSION_FINISH:
                            /* 停止任务 */
                            stopMission();
                            break;
                        case BasePublisher.Event.ON_MISSION_CLEAR:
                            /* 清除所有任务 */
                            for(BasePublisher basePublisher : mFollowWireMission.getPublishers()){
                                basePublisher.setTransactionHandler(null);
                            }
                            for(BaseHandler baseHandler : mFollowWireMission.getHandlers()){
                                baseHandler.setTransactionHandler(null);
                            }
                            mFollowWireMission = null;
                            mEventpublishThread.quitSafely();
                            break;
                    }
                    break;
            }
            return true;
        }

        private void publishTransactionToHandler(String event, Message msg, List<BaseHandler> baseHandlers){
            for(BaseHandler baseHandler : baseHandlers){
                Method[] methods = baseHandler.getClass().getMethods();
                for(Method method : methods){
                    try{
                        SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
                        if(subscribeEvent == null){
                            continue;
                        }
                        if(event.equals(subscribeEvent.event())){
                            method.invoke(baseHandler);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void publishTransactionToPublisher(String event, Message msg, List<BasePublisher> basePublishers){
            for(BasePublisher basePublisher : basePublishers){
                Method[] methods = basePublisher.getClass().getMethods();
                for(Method method : methods){
                    try{
                        SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
                        if(subscribeEvent == null){
                            continue;
                        }
                        if(event.equals(subscribeEvent.event())){
                            method.invoke(basePublisher);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    };

    public enum FollowWireMissionExecutorState {

        READY_TO_LOAD,

        READY_TO_START,

        EXECUTING,

        PAUSED;

    }

}
