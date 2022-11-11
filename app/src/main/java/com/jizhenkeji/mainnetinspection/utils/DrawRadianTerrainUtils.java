package com.jizhenkeji.mainnetinspection.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;

import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.model.entity.TerrainEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class DrawRadianTerrainUtils {
    /**
     * 创建切面视图表
     *
     * @param width        图表宽度
     * @param height       图表宽度
     */
    public static Bitmap createSectionView(int width, int height, TerrainEntity terrainEntity) {
        ByteArrayOutputStream sideViewPictureOutputStream = new ByteArrayOutputStream();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#ffffff"));
        float x=100f;
        float y=50f;
        canvas.translate(x,y);
        drawSectionView(canvas,terrainEntity,x,y);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, sideViewPictureOutputStream);
        return bitmap;
    }

    /**
     * 绘制切面图
     *
     * @param canvas
     */
    private static void drawSectionView(Canvas canvas,TerrainEntity terrainEntity,float x, float y) {
        DecimalFormat dfm  = new DecimalFormat("###.0");
        Paint paintxy = new Paint();//轴画笔
        float canvasWidth = canvas.getWidth();//长
        float canvasHeight = canvas.getHeight();//高
        float offsetX=x;//x偏移量
        float offsetY=y;//y偏移量
        float offsetInsideX=50f;//x偏移量
        float offsetInsideY=50f;//y偏移量
        float xLenght=getMaxX(terrainEntity.terrainInformations);
        float maxY=getMax(terrainEntity.terrainInformations);
        //第一次绘制坐标轴
        paintxy.setColor(0xff000000);//黑色
        canvas.drawLine(0, canvasHeight-offsetX, canvasWidth-2*offsetX, canvasHeight-offsetX, paintxy);//绘制x轴
        paintxy.setColor(0xff000000);//黑色
        canvas.drawLine(0, 0, 0, canvasHeight-2*offsetY, paintxy);//绘制y轴

        //绘制y轴箭头
        Path arrowyPath = new Path();
        arrowyPath.moveTo(0f, -4f);
        arrowyPath.lineTo(-4f, 0f);
        arrowyPath.lineTo(4f, 0f);
        arrowyPath.close();
        canvas.drawPath(arrowyPath, paintxy);

        //绘制y轴箭头
        Path arrowXPath = new Path();
        arrowXPath.moveTo(canvasWidth-2f*offsetX+4f, canvasHeight-2f*offsetY);
        arrowXPath.lineTo(canvasWidth-2f*offsetX, canvasHeight-2f*offsetY+4f);
        arrowXPath.lineTo(canvasWidth-2f*offsetX, canvasHeight-2f*offsetY-4f);
        arrowXPath.close();
        canvas.drawPath(arrowXPath, paintxy);

        //绘制x轴0刻度
        canvas.drawLine(offsetInsideX, canvasHeight-2*offsetY-10f, offsetInsideX, canvasHeight-2*offsetY, paintxy);
        canvas.drawText("0m", offsetInsideX, canvasHeight-2*offsetY+20f, paintxy);
        //绘制x轴1/4刻度
        canvas.drawLine((canvasWidth-2f*offsetX-2*offsetInsideX)/4+offsetInsideX, canvasHeight-2*offsetY-10f, (canvasWidth-2f*offsetX-2*offsetInsideX)/4+offsetInsideX, canvasHeight-2*offsetY, paintxy);
        canvas.drawText(dfm.format(xLenght/4)+"m", (canvasWidth-2f*offsetX-2*offsetInsideX)/4+offsetInsideX, canvasHeight-2*offsetY+20f, paintxy);
        //绘制x轴2/4刻度
        canvas.drawLine(((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*2+offsetInsideX, canvasHeight-2*offsetY-10f, ((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*2+offsetInsideX, canvasHeight-2*offsetY, paintxy);
        canvas.drawText(dfm.format(xLenght/3)+"m", ((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*2+offsetInsideX, canvasHeight-2*offsetY+20f, paintxy);
        //绘制x轴3/4刻度
        canvas.drawLine(((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*3+offsetInsideX, canvasHeight-2*offsetY-10f, ((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*3+offsetInsideX, canvasHeight-2*offsetY, paintxy);
        canvas.drawText(dfm.format(xLenght/2)+"m", ((canvasWidth-2f*offsetX-2*offsetInsideX)/4)*3+offsetInsideX, canvasHeight-2*offsetY+20f, paintxy);
        //绘制x轴大号塔刻度
        canvas.drawLine(canvasWidth-2f*offsetX-offsetInsideX, canvasHeight-2*offsetY-10f, canvasWidth-2f*offsetX-offsetInsideX, canvasHeight-2*offsetY, paintxy);
        canvas.drawText(dfm.format(xLenght)+"m", canvasWidth-2f*offsetX-offsetInsideX, canvasHeight-2*offsetY+20f, paintxy);


        //绘制y轴-35m刻度
        canvas.drawLine(10f, canvasHeight-3*offsetInsideY, 0f, canvasHeight-3*offsetInsideY, paintxy);
        canvas.drawText("-35m",-40f, canvasHeight-3*offsetInsideY, paintxy);

        //绘制y轴0号塔刻度
        canvas.drawLine(10f, ((canvasHeight-2*offsetY-offsetInsideY)/(maxY+35f))*maxY, 0f, ((canvasHeight-2*offsetY-offsetInsideY)/(maxY+35f))*maxY, paintxy);
        canvas.drawText("0m",-40f, ((canvasHeight-2*offsetY-offsetInsideY)/(maxY+35f))*maxY, paintxy);

        //绘制y轴xxm刻度
        canvas.drawLine(10f, 0f, 0f, 0f, paintxy);
        canvas.drawText(dfm.format(maxY)+"m",-40f, 0f, paintxy);

        Paint paintBarrier = new Paint();//轴画笔
        paintBarrier.setColor(0xffd81e06);//红色

        for(int a=0;a<terrainEntity.terrainInformations.size();a++) {



            //障碍物绘制
            canvas.drawLine(offsetInsideX + ((canvasWidth-2f*offsetX-2*offsetInsideX)/xLenght)*terrainEntity.terrainInformations.get(a).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY), offsetInsideX + ((canvasWidth-2f*offsetX-2*offsetInsideX)/xLenght)*terrainEntity.terrainInformations.get(a).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY)-((canvasHeight-2*offsetY-offsetInsideY)/(maxY+35))*(terrainEntity.terrainInformations.get(a).barrierDistance>35?35:terrainEntity.terrainInformations.get(a).barrierDistance), paintBarrier);

            if(a!=terrainEntity.terrainInformations.size()-1) {
                //导线绘制
                canvas.drawLine(offsetInsideX + ((canvasWidth-2*offsetX-2*offsetInsideX)/xLenght)*terrainEntity.terrainInformations.get(a).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY)-((canvasHeight-2*offsetY-offsetInsideY)/(maxY))*terrainEntity.terrainInformations.get(a).wireHeight, offsetInsideX + ((canvasWidth-2*offsetX-2*offsetInsideX)/xLenght)*terrainEntity.terrainInformations.get(a+1).trumpetTowerDistance, (canvasHeight - 2 * offsetY - offsetInsideY)-((canvasHeight-2*offsetY-offsetInsideY)/(maxY))*terrainEntity.terrainInformations.get(a+1).wireHeight, paintxy);
            }
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

    //获取最大数据
    public static float getMaxX(List<TerrainInformation> arr) {
        float max = arr.get(0).trumpetTowerDistance;
        for (int y = 1; y < arr.size(); y++) {
            if (arr.get(y).trumpetTowerDistance > max)
                max = arr.get(y).trumpetTowerDistance;
        }
        return max;

    }
}
