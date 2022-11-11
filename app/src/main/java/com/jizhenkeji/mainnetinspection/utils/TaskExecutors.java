package com.jizhenkeji.mainnetinspection.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全局线程池
 */
public class TaskExecutors {

    private static TaskExecutors INSTANCE;

    private final ScheduledExecutorService diskIO;

    private final Executor networkIO;

    private final MainThreadExecutor mainThread;


    public static TaskExecutors getInstance(){
        if(INSTANCE == null){
            INSTANCE =  new TaskExecutors();
        }
        return INSTANCE;
    }

    public TaskExecutors(){
        diskIO = Executors.newScheduledThreadPool(5);
        networkIO = Executors.newScheduledThreadPool(3);
        mainThread = new MainThreadExecutor();
    }

    /**
     * 本地文件读写线程
     * @param runnable
     */
    public void runInDiskIoThread(Runnable runnable){
        diskIO.execute(runnable);
    }

    /**
     * 本地文件读写线程，延时指定时间之后执行任务
     * @param runnable
     * @param delay 延时时间(毫秒)
     */
    public void runInDiskIoThread(Runnable runnable, long delay){
        diskIO.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 网络数据读写线程
     * @param runnable
     */
    public void runInNetworkIoThread(Runnable runnable){
        networkIO.execute(runnable);
    }

    /**
     * 运行在UI线程上
     * @param runnable
     */
    public void runInMainThread(Runnable runnable){
        mainThread.execute(runnable);
    }

    /**
     * 运行在UI线程上
     * @param runnable
     * @param delay 延时时间(毫秒)
     */
    public void runInMainThread(Runnable runnable, long delay){
        mainThread.execute(runnable, delay);
    }

    private static class MainThreadExecutor implements Executor {

        private Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }

        public void execute(Runnable command, long delay){
            mainHandler.postDelayed(command, delay);
        }

    }

}
