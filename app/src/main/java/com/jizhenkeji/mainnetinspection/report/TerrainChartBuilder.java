package com.jizhenkeji.mainnetinspection.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检的地形图表构建器
 */
public class TerrainChartBuilder {

    /**
     * 默认图表宽度
     */
    private final int DEFAULT_WIDTH = 780;

    /**
     * 默认图表高度
     */
    private final int DEFAULT_HEIGHT = 500;

    /**
     * 默认线宽度
     */
    private final int DEFAULT_LINE_WIDTH = 1;

    /**
     * 起始塔坐标
     */
    private Location mStartTowerLocation;

    /**
     * 终止塔坐标
     */
    private Location mEndTowerLocation;

    /**
     * 导线地形测量点列表
     */
    private List<TerrainInformation> mTerrainInformations;

    private double mFitA;

    private double mFitB;

    private double mFitC;

    private Context mContext;

    public TerrainChartBuilder(Context context, Location startTowerLocation, Location endTowerLocation, List<TerrainInformation> terrainInformations){
        mContext = context.getApplicationContext();
        mStartTowerLocation = startTowerLocation;
        mEndTowerLocation = endTowerLocation;
        mTerrainInformations = terrainInformations;
        /* 拟合一元二次方程 */
        WeightedObservedPoints weightedObservedPoints = new WeightedObservedPoints();
        for(TerrainInformation terrainInformation : mTerrainInformations){
            weightedObservedPoints.add(
                    GPSUtil.calculateLineDistance(mStartTowerLocation.latitude, mStartTowerLocation.longitude,
                            terrainInformation.aircraftLocation.latitude, terrainInformation.aircraftLocation.longitude),
                    terrainInformation.aircraftLocation.altitude
            );
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);
        double[] fitResult = fitter.fit(weightedObservedPoints.toList());
        mFitA = fitResult[2];
        mFitB = fitResult[1];
        mFitC = fitResult[0];
    }

    /**
     * 生成图表视图对象
     * @return
     */
    public View toView(){
        return toView(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 生成图表图片至对应文件
     * @param outFile 图片输出的文件对象
     * @param width 宽度
     * @param height 高度
     * @return 生成结果
     */
    public boolean toFile(File outFile, int width, int height){
        try{
            if(!outFile.exists() || !outFile.isFile()){
                outFile.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(outFile);
            boolean isSuccess = toOutputStream(outputStream, width, height);
            outputStream.close();
            return isSuccess;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 生成图表文件至对应输出流
     * @param outputStream 输出流
     * @param width 宽度
     * @param height 高度
     * @return
     */
    public boolean toOutputStream(OutputStream outputStream, int width, int height){
        View chartView = toView(width, height);
        Bitmap bitmap = Bitmap.createBitmap(chartView.getWidth(), chartView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        chartView.draw(canvas);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return true;
    }

    /**
     * 生成图表视图对象
     * @param width 宽度
     * @param height 高度
     * @return
     */
    private View toView(int width, int height){
        /* 创建图表视图对象 */
        LineChart chart = new LineChart(mContext);
        chart.setBackgroundColor(0xFFFFFFFF);
        /* 写入图表数据 */
        List<Entry> wireEntries = new ArrayList<>();
        List<Entry> barrierEntries = new ArrayList<>();

        float wireLength = GPSUtil.calculateLineDistance(
                mStartTowerLocation.latitude,
                mStartTowerLocation.longitude,
                mEndTowerLocation.latitude,
                mEndTowerLocation.longitude
        );
        /* 添加导线点数据 */
        for(float distanceToStart = 0; distanceToStart <= wireLength; distanceToStart += 0.5f){

            float aircraftAltitude = (float) (mFitA * Math.pow(distanceToStart, 2) + mFitB * distanceToStart + mFitC);
            wireEntries.add(new Entry(distanceToStart, aircraftAltitude));
        }
        /* 添加树障点数据 */
        for(TerrainInformation terrainInformation : mTerrainInformations){
            float distanceToStart = GPSUtil.calculateLineDistance(
                    mStartTowerLocation.latitude,
                    mStartTowerLocation.longitude,
                    terrainInformation.aircraftLocation.latitude,
                    terrainInformation.aircraftLocation.longitude
            );
            if(terrainInformation.barrierDistance > 0f){
                barrierEntries.add(new Entry(distanceToStart,
                        terrainInformation.aircraftLocation.altitude - terrainInformation.barrierDistance));
            }else{
                barrierEntries.add(new Entry(distanceToStart, 0));
            }
        }
        /* 设置图表样式 */
        LineDataSet wireDataSet = new LineDataSet(wireEntries, "导线");
        wireDataSet.setDrawCircles(false);
        wireDataSet.setLineWidth(DEFAULT_LINE_WIDTH);

        LineDataSet barrierDataSet = new LineDataSet(barrierEntries, "地形");
        barrierDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        barrierDataSet.setDrawCircles(false);
        barrierDataSet.setColor(Color.GREEN);
        barrierDataSet.setLineWidth(DEFAULT_LINE_WIDTH);
        barrierDataSet.setDrawFilled(true);
        barrierDataSet.setFillColor(Color.GREEN);
        /* 装载数据 */
        LineData lineData = new LineData();
        lineData.addDataSet(wireDataSet);
        lineData.addDataSet(barrierDataSet);
        chart.setData(lineData);
        /* 修改图表大小 */
        chart.layout(0, 0, width, height);
        return chart;
    }

}
