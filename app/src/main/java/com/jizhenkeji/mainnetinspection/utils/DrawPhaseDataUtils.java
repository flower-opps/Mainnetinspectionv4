package com.jizhenkeji.mainnetinspection.utils;

import static org.apache.poi.util.IOUtils.closeQuietly;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.dialog.PhaseSpacingDialogFragment;
import com.jizhenkeji.mainnetinspection.missionmanage.MissionManageActivity;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.TerrainEntity;
import com.jizhenkeji.mainnetinspection.report.TerrainChartBuilder;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class DrawPhaseDataUtils {
    public static float Data_fitting_a = 0, Data_fitting_b = 0, Data_fitting_c = 0;
    public static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    public static List<Float> fts = new ArrayList<>();
    private static InputStream templateInputStream;
    private static XWPFDocument document;
    private static List<XWPFParagraph> paragraphs;
    private static List<XWPFTable> tables;

    /**
     * 创建切面视图表
     *
     * @param width  图表宽度
     * @param height 图表宽度
     */
    public static Bitmap createSectionView(int width, int height, ArrayList<DataWithMetadata> dataWithMetadata, float dis) {
        ByteArrayOutputStream sideViewPictureOutputStream = new ByteArrayOutputStream();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#ffffff"));
        float x = 100f;
        float y = 50f;
        canvas.translate(x, y);
        drawSectionView(canvas, dataWithMetadata, x, y, dis);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, sideViewPictureOutputStream);
        ByteArrayInputStream terrainChartInputStream = new ByteArrayInputStream(sideViewPictureOutputStream.toByteArray());

        XWPFTable barrierChartTable = tables.get(5);
        XWPFTableCell barrierChartCell = barrierChartTable.getRow(0).getCell(0);
        XWPFRun barrierChartRun = barrierChartCell.getParagraphArray(0).createRun();
        /* 保存文件 */
        FileOutputStream reportOutputStream = null;
        try {
            barrierChartRun.addPicture(terrainChartInputStream, XWPFDocument.PICTURE_TYPE_JPEG, "", Units.toEMU(500), Units.toEMU(300));
            reportOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/JZI/相间测距/" + dataWithMetadata.get(0).dataEntity.missionName + "相间测距报告.docx"));
            document.write(reportOutputStream);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /**
     * 绘制切面图
     *
     * @param canvas
     */
    private static void drawSectionView(Canvas canvas, ArrayList<DataWithMetadata> dataWithMetadata, float x, float y, float dis) {
        DecimalFormat dfm = new DecimalFormat("###.0");
        Paint paintxy = new Paint();//轴画笔
        Paint paintot = new Paint();//其它
        Paint paintdx = new Paint();//导线
        Paint painttext = new Paint();//字
        painttext.setTextSize(30);
        float canvasWidth = canvas.getWidth();//长
        float canvasHeight = canvas.getHeight();//高
        float offsetX = x;//x偏移量
        float offsetY = y;//y偏移量
        float offsetInsideX = 50f;//x偏移量
        float offsetInsideY = 50f;//y偏移量
        List<TerrainInformation> arr = new ArrayList<>();
        try {
            for (int a = 0; a < dataWithMetadata.size(); a++) {
                for (int b = 0; b < dataWithMetadata.get(a).terrainEntity.terrainInformations.size(); b++) {
                    arr.add(dataWithMetadata.get(a).terrainEntity.terrainInformations.get(b));
                }
            }

            List<Float> floats = new ArrayList<>();
            for (int ll = 0; ll < dataWithMetadata.size(); ll++) {
                floats.add((float) dataWithMetadata.get(ll).terrainEntity.terrainInformations.get(dataWithMetadata.get(ll).terrainEntity.terrainInformations.size() - 1).trumpetTowerDistance);
            }
            float xLenght = dis;
            float maxY = 2.0f * getMax(arr);
            //第一次绘制坐标轴
            paintxy.setStrokeWidth(2.0f);
            paintxy.setColor(0xff000000);//黑色
            paintot.setColor(0xff000000);//黑色
            paintdx.setColor(0xff666666);
            canvas.drawLine(0, canvasHeight - offsetX, canvasWidth - 2 * offsetX, canvasHeight - offsetX, paintxy);//绘制x轴
            canvas.drawLine(0, 0, 0, canvasHeight - 2 * offsetY, paintxy);//绘制y轴

            //绘制y轴箭头
            Path arrowyPath = new Path();
            arrowyPath.moveTo(0f, -4f);
            arrowyPath.lineTo(-4f, 0f);
            arrowyPath.lineTo(4f, 0f);
            arrowyPath.close();
            canvas.drawPath(arrowyPath, paintxy);

            //绘制y轴箭头
            Path arrowXPath = new Path();
            arrowXPath.moveTo(canvasWidth - 2f * offsetX + 4f, canvasHeight - 2f * offsetY);
            arrowXPath.lineTo(canvasWidth - 2f * offsetX, canvasHeight - 2f * offsetY + 4f);
            arrowXPath.lineTo(canvasWidth - 2f * offsetX, canvasHeight - 2f * offsetY - 4f);
            arrowXPath.close();
            canvas.drawPath(arrowXPath, paintxy);

            //绘制x轴小号塔
            canvas.drawLine(offsetInsideX, canvasHeight - 2 * offsetY - 10f, offsetInsideX, canvasHeight - 2 * offsetY, paintot);
            canvas.drawText(dataWithMetadata.get(0).dataEntity.startTowerNum + "号塔", offsetInsideX, canvasHeight - 2 * offsetY + 20f, paintot);
            //绘制x轴大号塔
            canvas.drawLine(canvasWidth - 2f * offsetX - offsetInsideX, canvasHeight - 2 * offsetY - 10f, canvasWidth - 2f * offsetX - offsetInsideX, canvasHeight - 2 * offsetY, paintot);
            canvas.drawText(dataWithMetadata.get(0).dataEntity.endTowerNum + "号塔", canvasWidth - 2f * offsetX - offsetInsideX, canvasHeight - 2 * offsetY + 20f, paintot);

            canvas.drawText("相间测距图", canvasWidth / 2 - offsetInsideX - offsetX, 0, painttext);

            //绘制y轴RTK高度
//        canvas.drawLine(10f, 0f, 0f, 0f, paintot);
//        canvas.drawText(dfm.format(maxY)+"m",-40f, 0f, paintot);
            Paint paintBarrier = new Paint();//轴画笔
            paintBarrier.setColor(0xffd81e06);//红色

            for (int c = 0; c < dataWithMetadata.size(); c++) {
                Data_fitting(dataWithMetadata.get(c).terrainEntity.terrainInformations);
                for (int d = 0; d < dataWithMetadata.get(c).terrainEntity.terrainInformations.size(); d++) {
                    if (d == 0) {
                        float radius = Math.min(5.0f / 2.0f, 5.0f / 2.0f);
                        canvas.drawCircle(offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, radius, paintot);
                        //绘制y轴xxm刻度
                        canvas.drawLine(10f, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, 0f, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, paintot);
                        canvas.drawText(dfm.format(dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight) + "m", -40f, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, paintot);
                        //1. convert all (x,y) points from meter to (px, py)

                        //2. calc a\b\c for y = ax^2+bx+c

                        //3. get path draw px_start、px_end

                        //4. calc pyi = a*pxi^2+b*pxi+c, pxi between [px_start,px_end]

                        //5. draw all (pxi, pyi) points
                    }
                    if (d != dataWithMetadata.get(c).terrainEntity.terrainInformations.size() - 1) {
                        //导线绘制
                        canvas.drawLine(offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance, (float) ((canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * (Math.pow(dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance, 2) * Data_fitting_a + Data_fitting_b * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance + Data_fitting_c)), offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * (dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).trumpetTowerDistance * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).trumpetTowerDistance * Data_fitting_a + Data_fitting_b * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).trumpetTowerDistance + Data_fitting_c), paintdx);
//                    canvas.drawLine(offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d + 1).wireHeight, paintxy);
                    }
                    if (d == dataWithMetadata.get(c).terrainEntity.terrainInformations.size() - 1) {
                        float radius = Math.min(5.0f / 2.0f, 5.0f / 2.0f);
                        canvas.drawCircle(offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * dataWithMetadata.get(c).terrainEntity.terrainInformations.get(d).wireHeight, radius, paintot);
                    }
                }
            }
            for (int ii = 0; ii < dataWithMetadata.size() - 1; ii++) {
                for (int jj = 0; jj < dataWithMetadata.size() - ii - 1; jj++) {
                    if (dataWithMetadata.get(jj).terrainEntity.terrainInformations.get(0).wireHeight < dataWithMetadata.get(jj + 1).terrainEntity.terrainInformations.get(0).wireHeight) {
                        DataWithMetadata data = dataWithMetadata.get(jj);
                        dataWithMetadata.set(jj, dataWithMetadata.get(jj + 1));
                        dataWithMetadata.set(jj + 1, data);
                    }

                }
            }
            for (int f = 0; f < dataWithMetadata.size() - 1; f++) {
                float distance = 0f;
                float xx1 = 0f;
                float xx2 = 0f;
                float yy1 = 0f;
                float yy2 = 0f;
                for (int h = 0; h < dataWithMetadata.get(f).terrainEntity.terrainInformations.size(); h++) {
                    for (int k = 0; k < dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.size(); k++) {
                        if (Math.ceil(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).trumpetTowerDistance) == Math.ceil(dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).trumpetTowerDistance)) {
                            if (distance == 0f || distance > Float.parseFloat(dfm.format(Math.abs(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).wireHeight - dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).wireHeight)))) {
                                distance = Float.parseFloat(dfm.format(Math.abs(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).wireHeight - dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).wireHeight)));
                                xx1 = dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).trumpetTowerDistance;
                                xx2 = dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).trumpetTowerDistance;
                                yy1 = dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).wireHeight;
                                yy2 = dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).wireHeight;
                            }
                        }
                    }
                }
                canvas.drawLine(offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * xx1, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * yy1, offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * xx1, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * yy2, paintot);
                canvas.drawText("高度:" + distance, offsetInsideX + ((canvasWidth - 2 * offsetX - 2 * offsetInsideX) / xLenght) * xx1 + 10f, (canvasHeight - 2 * offsetY - offsetInsideY) - ((canvasHeight - 2 * offsetY - offsetInsideY) / (maxY)) * (yy1 > yy2 ? yy2 : yy1), paintot);
                fts.add(distance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            /* 读取树障报告模板文件 */
            templateInputStream = GlobalUtils.getAssets().open("reportPhase.docx");
            document = new XWPFDocument(templateInputStream);
            /* 获取模板文件所有的段落和表格 */
            paragraphs = document.getParagraphs();
            tables = document.getTables();
            /* 写入报告名称 */
            XWPFParagraph titleParagraph = paragraphs.get(0);
            titleParagraph.getRuns().get(0).setText(dataWithMetadata.get(0).dataEntity.missionName, 0);
            /* 写入报告日期 */
            XWPFParagraph dateParagraph = paragraphs.get(7);
            dateParagraph.getRuns().get(0).setText(df.format(new Date()), 0);
            /* 写入线路基本信息 */
            XWPFTable lineInfoTable = tables.get(0);
            lineInfoTable.getRow(0).getCell(1).setText(null != dataWithMetadata.get(0).dataEntity.missionName ? dataWithMetadata.get(0).dataEntity.missionName : "");                       // 线路名称
            lineInfoTable.getRow(0).getCell(3).setText(null != dataWithMetadata.get(0).dataEntity.voltageLevel ? dataWithMetadata.get(0).dataEntity.voltageLevel : "");                     // 电压等级
            lineInfoTable.getRow(1).getCell(1).setText(null != dataWithMetadata.get(0).dataEntity.manageClassName ? dataWithMetadata.get(0).dataEntity.manageClassName : "");               // 管辖班组
            lineInfoTable.getRow(2).getCell(3).setText(dataWithMetadata.get(0).dataEntity.treeBarrierThreshold + "");               // 保护区范围
            lineInfoTable.getRow(2).getCell(1).setText(dataWithMetadata.get(0).dataEntity.startTowerNum + "-" + dataWithMetadata.get(0).dataEntity.endTowerNum);               // 扫描塔范围
            lineInfoTable.getRow(3).getCell(1).setText(df.format(dataWithMetadata.get(0).dataEntity.createDate));               // 扫描塔范围
            /* 写入无人机相关信息 */
            XWPFTable aircraftInfoTable = tables.get(1);
            aircraftInfoTable.getRow(1).getCell(0).setText(null != dataWithMetadata.get(0).dataEntity.aircraftName ? dataWithMetadata.get(0).dataEntity.aircraftName : "");                 // 飞机型号
            /* 写入作业情况表格 */
            XWPFTable workInfoTable = tables.get(2);
            workInfoTable.getRow(1).getCell(1)
                    .setText(dataWithMetadata.get(0).dataEntity.startTowerNum + "-" + dataWithMetadata.get(0).dataEntity.endTowerNum);                                                             // 数据采集范围
            workInfoTable.getRow(1).getCell(2).setText(df.format(dataWithMetadata.get(0).dataEntity.createDate != null ? dataWithMetadata.get(0).dataEntity.createDate : new Date())); // 采集日期                    // 采集日期
            workInfoTable.getRow(1).getCell(3).setText("");                                                         // 温度
            workInfoTable.getRow(1).getCell(4).setText("");                                                         // 风速
            /* 写入巡检结果统计表格 */
            XWPFTable xjjgTable = tables.get(4);
            int wow = 0;
            for (int pop = 0; pop < fts.size(); pop++) {
                if (fts.get(pop) < 4.0f) {
                    wow++;
                }
            }
            xjjgTable.getRow(1).getCell(1).setText(String.valueOf(fts.size()));                           // 障碍个数
            xjjgTable.getRow(1).getCell(2).setText(String.valueOf(wow));                           // 障碍个数
            XWPFParagraph fwParagraph = paragraphs.get(38);
            fwParagraph.getRuns().get(0).setText("扫描塔号范围" + dataWithMetadata.get(0).dataEntity.startTowerNum + "-" + dataWithMetadata.get(0).dataEntity.endTowerNum, 0);


            XWPFTable inspectionTable = tables.get(6);
            for (int fox = 0; fox < dataWithMetadata.size() - 1; fox++) {
                inspectionTable.getRow(fox + 1).getCell(1).setText(dataWithMetadata.get(fox).dataEntity.phaseNumber + "与" + dataWithMetadata.get(fox).dataEntity.phaseNumber);// 共检测项
                inspectionTable.getRow(fox + 1).getCell(2).setText(dataWithMetadata.get(fox).dataEntity.location.longitude + "");// 共检测项
                inspectionTable.getRow(fox + 1).getCell(3).setText(dataWithMetadata.get(fox).dataEntity.location.latitude + "");// 共检测项
                inspectionTable.getRow(fox + 1).getCell(4).setText(fts.get(fox) + "");// 共检测项
                inspectionTable.getRow(fox + 1).getCell(5).setText(dfm.format(getDistance(dataWithMetadata.get(fox).dataEntity.endPointLocation.longitude, dataWithMetadata.get(fox).dataEntity.endPointLocation.latitude, dataWithMetadata.get(fox).dataEntity.startPointLocation.longitude, dataWithMetadata.get(fox).dataEntity.startPointLocation.latitude)) + "");// 共检测项
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            templateInputStream = GlobalUtils.getAssets().open("reportPhase.xlsx");
            Workbook mExcelWorkbook = new XSSFWorkbook(templateInputStream);

            Sheet sheet=mExcelWorkbook.getSheet("Sheet1");

            Row titleRow1=sheet.createRow(0);
            titleRow1.setHeightInPoints(60);

            Cell cell0=titleRow1.createCell(0);
            cell0.setCellValue("相号");
            Cell cell1=titleRow1.createCell(1);
            cell1.setCellValue("小号塔距离");
            Cell cell2=titleRow1.createCell(2);
            cell2.setCellValue("距离");

            int rownum=1;
            for (int f = 0; f < dataWithMetadata.size()-1; f++) {
                for (int h = 0; h < dataWithMetadata.get(f).terrainEntity.terrainInformations.size(); h+=20) {
                    for (int k = 0; k < dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.size(); k+=20) {
                        if (Math.ceil(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).trumpetTowerDistance) == Math.ceil(dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).trumpetTowerDistance)) {
                            Row titleRow2=sheet.createRow(rownum);
                            titleRow2.setHeightInPoints(60);
                            Cell cell00=titleRow2.createCell(0);
                            cell00.setCellValue(dataWithMetadata.get(f).dataEntity.phaseNumber+"-"+dataWithMetadata.get(f+1).dataEntity.phaseNumber);
                            Cell cell11=titleRow2.createCell(1);
                            cell11.setCellValue(dfm.format(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).trumpetTowerDistance));
                            Cell cell22=titleRow2.createCell(2);
                            cell22.setCellValue(dfm.format(Math.abs(dataWithMetadata.get(f).terrainEntity.terrainInformations.get(h).wireHeight - dataWithMetadata.get(f + 1).terrainEntity.terrainInformations.get(k).wireHeight)));
                            rownum++;
                        }
                    }
                }
            }
            for (int hh = 0; hh < dataWithMetadata.get(0).terrainEntity.terrainInformations.size(); hh+=20) {
                for (int kk = 0; kk < dataWithMetadata.get(dataWithMetadata.size()-1).terrainEntity.terrainInformations.size(); kk+=20) {
                    if (Math.ceil(dataWithMetadata.get(0).terrainEntity.terrainInformations.get(hh).trumpetTowerDistance) == Math.ceil(dataWithMetadata.get(dataWithMetadata.size()-1).terrainEntity.terrainInformations.get(kk).trumpetTowerDistance)) {
                        Row titleRow2=sheet.createRow(rownum);
                        titleRow2.setHeightInPoints(60);
                        Cell cell00=titleRow2.createCell(0);
                        cell00.setCellValue(dataWithMetadata.get(0).dataEntity.phaseNumber+"-"+dataWithMetadata.get(dataWithMetadata.size()-1).dataEntity.phaseNumber);
                        Cell cell11=titleRow2.createCell(1);
                        cell11.setCellValue(dfm.format(dataWithMetadata.get(0).terrainEntity.terrainInformations.get(hh).trumpetTowerDistance));
                        Cell cell22=titleRow2.createCell(2);
                        cell22.setCellValue(dfm.format(Math.abs(dataWithMetadata.get(0).terrainEntity.terrainInformations.get(hh).wireHeight - dataWithMetadata.get(dataWithMetadata.size()-1).terrainEntity.terrainInformations.get(kk).wireHeight)));
                        rownum++;
                    }
                }
            }

            FileOutputStream reportOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "/JZI/相间测距/" + dataWithMetadata.get(0).dataEntity.missionName + "相间测距.xlsx"));
            mExcelWorkbook.write(reportOutputStream);
            mExcelWorkbook.close();

        }catch (Exception e){
            Log.d("ssdgfsadgd",e+"");

        }

    }

    //获取最大数据
    public static float getMax(List<TerrainInformation> arr) {
        float max = arr.get(0).wireHeight;
        for (int x = 1; x < arr.size(); x++) {
            if (arr.get(x).wireHeight > max)
                max = arr.get(x).wireHeight;
        }
        return max;

    }

    //-----------------一元二次方程数据拟合算法------------//
    public static void Data_fitting(List<TerrainInformation> terrainInformation) {
        try {
            final WeightedObservedPoints obs = new WeightedObservedPoints();
            for (int i = 0; i < terrainInformation.size(); i++) {
                float X = terrainInformation.get(i).trumpetTowerDistance;
                float Y = terrainInformation.get(i).wireHeight;
                obs.add(X, Y);
            }
            final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
            final double[] coeff = fitter.fit(obs.toList());
            Data_fitting_a = (float) coeff[2];
            Data_fitting_b = (float) coeff[1];
            Data_fitting_c = (float) coeff[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double getDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
        // 纬度
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        // 经度
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 米
        s = s * 6378137;
        return s;
    }

}
