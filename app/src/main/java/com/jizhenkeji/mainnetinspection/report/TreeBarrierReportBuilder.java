package com.jizhenkeji.mainnetinspection.report;

import androidx.annotation.NonNull;

import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TreeBarrierReportBuilder {

    private String mMissionName;

    private Date mReportDate;

    private String mVoltageLevel;

    private int mStartTowerNum;

    private Location mStartTowerLocation;

    private int mEndTowerNum;

    private Location mEndTowerLocation;

    private Date mInspectionDate;

    private int mTreeBarrierDistance;

    private String mPhaseNumber;

    private String mManageClassName;

    private String mAircraftName;

    private List<String> mTreeBarrierName = new ArrayList<>();

    private List<String> mTreeBarrierDescription = new ArrayList<>();

    private List<Location> mTreeBarrierLocations = new ArrayList<>();

    private List<Float> mTreeBarrierHorizontalDistances = new ArrayList<>();

    private List<Float> mTreeBarrierVerticalDistance = new ArrayList<>();

    private List<InputStream> mTreeBarrierPhotoInputStream = new ArrayList<>();

    private List<TerrainInformation> mTerrainInformations = new ArrayList<>();

    /**
     * 设置任务名称
     * @param name
     */
    public void setMissionName(String name){
        mMissionName = name;
    }

    /**
     * 设置任务日期
     * @param date
     */
    public void setDate(Date date){
        mReportDate = date;
    }

    /**
     * 设置电压等级
     * @param voltageLevel
     */
    public void setVoltageLevel(String voltageLevel){
        mVoltageLevel = voltageLevel;
    }

    /**
     * 设置起始杆塔信息
     * @param towerNum 起始杆塔号
     * @param location
     */
    public void setStartTowerInformation(int towerNum, @NonNull Location location){
        mStartTowerNum = towerNum;
        mStartTowerLocation = location;
    }

    /**
     * 设置终止杆塔信息
     * @param towerNum
     * @param location
     */
    public void setEndTowerInformation(int towerNum, Location location){
        mEndTowerNum = towerNum;
        mEndTowerLocation = location;
    }

    /**
     * 设置地形数据
     * @param terrainInformations
     */
    public void setTerrainInformations(List<TerrainInformation> terrainInformations){
        mTerrainInformations = terrainInformations;
    }

    public void setInspectionDate(Date date){
        mInspectionDate = date;
    }

    public void setTreeBarrierDistance(int distance){
        mTreeBarrierDistance = distance;
    }

    public void setPhaseNumber(String phaseNumber){
        mPhaseNumber = phaseNumber;
    }

    public void setManageClassName(String className){
        mManageClassName = className;
    }

    public void setAircraftName(String aircraftName){
        mAircraftName = aircraftName;
    }

    public void addTreeBarrierInformation(
            @NonNull String name,
            @NonNull String description,
            @NonNull Location location,
            float horizontalDistance,
            float verticalDistance,
            @NonNull InputStream treeBarrierInputStream){
        mTreeBarrierName.add(name);
        mTreeBarrierDescription.add(description);
        mTreeBarrierLocations.add(location);
        mTreeBarrierHorizontalDistances.add(horizontalDistance);
        mTreeBarrierVerticalDistance.add(verticalDistance);
        mTreeBarrierPhotoInputStream.add(treeBarrierInputStream);
    }

    /**
     * 等待地形表格生成标志位
     */
    private volatile boolean isWaitTerrainChart = true;

    public void build(OutputStream outputStream, TreeBarrierReportCallback callback){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            if(callback != null) callback.onStart();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            /* 计算两座杆塔档距 */
            float wireLength = 0f;
            if(mEndTowerLocation != null){
                wireLength = GPSUtil.calculateLineDistance(
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude,
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude
                );
            }
            try{
                /* 读取树障报告模板文件 */
                InputStream templateInputStream = GlobalUtils.getAssets().open("reportTemp.docx");
                XWPFDocument document = new XWPFDocument(templateInputStream);
                /* 获取模板文件所有的段落和表格 */
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                List<XWPFTable> tables = document.getTables();
                /* 写入报告名称 */
                XWPFParagraph titleParagraph = paragraphs.get(3);
                titleParagraph.getRuns().get(0).setText(mMissionName != null ? mMissionName : "", 0);
                /* 写入报告日期 */
                XWPFParagraph dateParagraph = paragraphs.get(16);
                dateParagraph.getRuns().get(0).setText(df.format(mReportDate != null ? mReportDate : new Date()), 0);
                /* 写入线路基本信息 */
                XWPFTable lineInfoTable = tables.get(0);
                lineInfoTable.getRow(0).getCell(1).setText(mMissionName != null ? mMissionName : "");                       // 线路名称
                lineInfoTable.getRow(0).getCell(3).setText(mVoltageLevel != null ? mVoltageLevel : "");                     // 电压等级
                lineInfoTable.getRow(1).getCell(1).setText(String.valueOf(wireLength));                                     // 档距
                lineInfoTable.getRow(2).getCell(3).setText(df.format(mReportDate != null ? mReportDate : new Date()));      // 报告日期
                lineInfoTable.getRow(3).getCell(1).setText(String.valueOf(mStartTowerNum));                                 // 起始杆塔号
                lineInfoTable.getRow(3).getCell(3).setText(String.valueOf(mEndTowerNum));                                   // 终止杆塔号
                lineInfoTable.getRow(4).getCell(1).setText(df.format(mInspectionDate != null ? mInspectionDate : new Date()));  // 采集日期
                lineInfoTable.getRow(4).getCell(3).setText(String.valueOf(mTreeBarrierDistance));                           // 保护范围
                lineInfoTable.getRow(5).getCell(1).setText(mPhaseNumber != null ? mPhaseNumber : "");                       // 相号
                lineInfoTable.getRow(5).getCell(3).setText(mManageClassName != null ? mManageClassName : "");               // 管辖班组
                /* 写入无人机相关信息 */
                XWPFTable aircraftInfoTable = tables.get(1);
                aircraftInfoTable.getRow(1).getCell(0).setText(mAircraftName != null ? mAircraftName : "");                 // 飞机型号
                /* 写入作业情况表格 */
                XWPFTable workInfoTable = tables.get(2);
                workInfoTable.getRow(1).getCell(1)
                        .setText(mStartTowerNum + "-" + mEndTowerNum);                                                             // 数据采集范围
                workInfoTable.getRow(1).getCell(2).setText(df.format(mInspectionDate != null ? mInspectionDate : new Date())); // 采集日期                    // 采集日期
                workInfoTable.getRow(1).getCell(3).setText("");                                                         // 温度
                workInfoTable.getRow(1).getCell(4).setText("");                                                         // 风速
                /* 写入巡检结果统计表格 */
                int treeBarrierNum = mTreeBarrierLocations.size();
                XWPFTable inspectionTable = tables.get(4);
                inspectionTable.getRow(1).getCell(1).setText(String.valueOf(treeBarrierNum));                           // 共检测项
                inspectionTable.getRow(1).getCell(2).setText(String.valueOf(treeBarrierNum));                           // 障碍个数
                /* 通道巡检结果明细表格 */
                XWPFTable inspectionDetailTable = tables.get(5);
                inspectionDetailTable.getRow(0).getCell(1).setText(String.valueOf(mTreeBarrierDistance));               // 树障阈值
                /* 树障相关表格 */
                XWPFTable treeBarrierTable = tables.get(6);
                XWPFTable treeBarrierDetailTable = tables.get(7);
                for(int i = 0; i < mTreeBarrierPhotoInputStream.size(); i++){
                    /* 树障信息表格 */
                    Location treeBarrierLocation = mTreeBarrierLocations.get(i);
                    float distanceToStartTower = GPSUtil.calculateLineDistance(
                            mStartTowerLocation.latitude,
                            mStartTowerLocation.longitude,
                            treeBarrierLocation.latitude,
                            treeBarrierLocation.longitude
                    );
                    XWPFTableRow infoRow = treeBarrierTable.createRow();
                    infoRow.getCell(0).setText(String.valueOf(i));
                    infoRow.getCell(1).setText("树障");
                    infoRow.getCell(2).setText(String.valueOf(treeBarrierLocation.longitude));
                    infoRow.getCell(3).setText(String.valueOf(treeBarrierLocation.latitude));
                    infoRow.getCell(4).setText(String.valueOf(mTreeBarrierHorizontalDistances.get(i)));
                    infoRow.getCell(5).setText(String.valueOf(mTreeBarrierVerticalDistance.get(i)));
                    infoRow.getCell(6).setText(String.valueOf(distanceToStartTower));
                    /* 树障详细表格 */
                    XWPFTableRow detailRow = treeBarrierDetailTable.createRow();
                    detailRow.getCell(0).setText(mTreeBarrierName.get(i));
                    detailRow.getCell(1).setText(mTreeBarrierDescription.get(i));
                    XWPFParagraph treeBarrierPicParagraph = detailRow.getCell(2).addParagraph();
                    XWPFRun treeBarrierPicRun = treeBarrierPicParagraph.createRun();
                    treeBarrierPicRun.addPicture(mTreeBarrierPhotoInputStream.get(i), XWPFDocument.PICTURE_TYPE_JPEG, "", Units.toEMU(400), Units.toEMU(256));
                    /* 计算生成进度 */
                    int progress = i * 100 / mTreeBarrierPhotoInputStream.size();
                    String progressMessage = "正在处理" + mTreeBarrierName.get(i) + "树障图片";
                    if(callback != null) callback.onProgress(progress, progressMessage);
                }
                /* 生成障碍图表 */
                if(mEndTowerLocation != null){
                    ByteArrayOutputStream terrainChartOutputStream = new ByteArrayOutputStream();
                    TerrainChartBuilder terrainChartBuilder = new TerrainChartBuilder(
                            GlobalUtils.getApplicationContext(),
                            mStartTowerLocation,
                            mEndTowerLocation,
                            mTerrainInformations);
                    TaskExecutors.getInstance().runInMainThread(() -> {
                        terrainChartBuilder.toOutputStream(terrainChartOutputStream, 1000, 600);
                        isWaitTerrainChart = false;
                    });
                    while (isWaitTerrainChart);
                    ByteArrayInputStream terrainChartInputStream = new ByteArrayInputStream(terrainChartOutputStream.toByteArray());
                    XWPFTable barrierChartTable = tables.get(8);
                    XWPFTableCell barrierChartCell = barrierChartTable.getRow(0).getCell(0);
                    XWPFRun barrierChartRun = barrierChartCell.getParagraphArray(0).createRun();
                    barrierChartRun.addPicture(terrainChartInputStream, XWPFDocument.PICTURE_TYPE_JPEG, "", Units.toEMU(500), Units.toEMU(300));
                }
                /* 保存文件 */
                document.write(outputStream);
                document.close();
                if(callback != null) callback.onFinish(null);
            } catch (IOException | InvalidFormatException e) {
                if(callback != null) callback.onFinish(JZIError.FAIL_TO_BUILD_REPORT);
            }
        });
    }

    public interface TreeBarrierReportCallback {

        void onStart();

        void onProgress(int progress, String msg);

        void onFinish(JZIError jziError);

    }

}
