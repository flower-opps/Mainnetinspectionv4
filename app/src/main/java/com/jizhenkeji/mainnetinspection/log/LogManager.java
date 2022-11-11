package com.jizhenkeji.mainnetinspection.log;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LogManager {

    private final String TAG = "LoggingManager";

    public static LogManager getInstance(){
        return Manager.INSTANCE;
    }

    private static class Manager {
        private static final LogManager INSTANCE = new LogManager();
    }

    private LogManager(){}

    /* 软件日志超时时间，单位：天 */
    public static final int LOG_OVERDUE_TIME = 5;

    /* 存放日志文件的文件夹名称 */
    private final String LOG_PATH_NAME = "logs";

    private File mLogRootPath;

    private File mLogFile;

    private Context mApplicationContext;

    /**
     * 初始化日志管理器
     * @param context 应用上下文
     */
    public void init(Context context, @NonNull File logRootPath){
        mApplicationContext = context.getApplicationContext();

        if(!logRootPath.exists() || !logRootPath.isDirectory()){
            logRootPath.mkdir();
        }
        mLogRootPath = logRootPath;
        if(!mLogRootPath.exists() || !mLogRootPath.isDirectory()){
            mLogRootPath.mkdir();
        }

        deleteOverdueLog();         // 删除过期日志

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        mLogFile = new File(mLogRootPath, dateFormat.format(new Date()) + ".log");
        try{
            Runtime.getRuntime().exec("logcat -f " + mLogFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除过期日志
     */
    private void deleteOverdueLog(){
        File[] logFiles = mLogRootPath.listFiles();
        if(logFiles == null || logFiles.length == 0){
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, - LOG_OVERDUE_TIME);
        Date overdueDate = calendar.getTime();
        for(File logFile:logFiles){
            Date logModifyTime = new Date(logFile.lastModified());
            if(logModifyTime.before(overdueDate)){
                logFile.delete();
            }
        }
    }

}
