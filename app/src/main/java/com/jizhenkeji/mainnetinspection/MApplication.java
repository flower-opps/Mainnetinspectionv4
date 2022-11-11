package com.jizhenkeji.mainnetinspection;

import android.content.Context;
import android.os.Environment;

import com.jizhenkeji.mainnetinspection.JZIApplication;
import com.jizhenkeji.mainnetinspection.log.LogManager;

import java.io.File;

public class MApplication extends JZIApplication {

    /* 用于初始化编辑Word库的部分变量 */
    static {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
    }

    /**
     * 软件版本
     */
    public static final String VERSION = "v2.0.0";

    /**
     * 远程服务根地址
     */
    public static final String SERVICE_ROOT_URL = "http://192.168.5.34:8848/UpdateService";

    /**
     * 文件内容提供者标识
     */
    public static final String FILE_PROVIDER_AUTHORITY = "com.jizhenkeji.mainnetinspection.fileprovider";

    /**
     * 软件文件夹目录名称
     */
    public static final String JZI_PATH_NAME  = "JZI";

    /**
     * 软件日志文件夹目录名称
     */
    public static final String LOG_PATH_NAME = "logs";

    /**
     * 任务台账数据文件夹目录名称
     */
    public static final String MISSION_PATH_NAME = "台账数据";

    /**
     * 存储软件配置文件的目录
     */
    public static final File JZI_ROOT_PATH = new File(Environment.getExternalStorageDirectory(), JZI_PATH_NAME);

    /**
     * 任务台账数据文件夹目录
     */
    public static final File MISSION_ROOT_PATH = new File(JZI_ROOT_PATH, MISSION_PATH_NAME);

    /**
     * 存储日志文件夹的目录
     */
    public static final File LOG_ROOT_PATH = new File(JZI_ROOT_PATH, LOG_PATH_NAME);

    @Override
    public void onCreate() {
        super.onCreate();
        /* 初始化日志管理对象 */
        LogManager.getInstance().init(getApplicationContext(), LOG_ROOT_PATH);
        /* 创建配置文件根目录 */
        if(!JZI_ROOT_PATH.exists() || !JZI_ROOT_PATH.isDirectory()){
            JZI_ROOT_PATH.mkdir();
        }
    }

}
