package com.jizhenkeji.mainnetinspection.model.converter;

import androidx.room.TypeConverter;

import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import dji.sdk.media.MediaFile;

public class DataConverters {

    @TypeConverter
    public static byte[] terrainInformationsToBytes(List<TerrainInformation> terrainInformations){
        return objectToBytes(terrainInformations);
    }

    @TypeConverter
    public static List<TerrainInformation> bytesToTerrainInformations(byte[] data){
        return bytesToObject(data);
    }

    @TypeConverter
    public static byte[] cameraMediaTypeToBytes(CameraMediaFileType cameraMediaFileType){
        return objectToBytes(cameraMediaFileType);
    }

    @TypeConverter
    public static CameraMediaFileType bytesToCameraMedia(byte[] data){
        return bytesToObject(data);
    }

    @TypeConverter
    public static byte[] mediaFileToBytes(MediaFile mediaFile){
        return objectToBytes(mediaFile);
    }

    @TypeConverter
    public static MediaFile bytesToMediaFile(byte[] data){
        return bytesToObject(data);
    }

    public static <T> byte[] objectToBytes(T object){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            /* 将媒体文件列表序列化写入数据库 */
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            objectOutputStream.close();
        }catch (IOException e){
            return new byte[0];
        }
        return outputStream.toByteArray();
    }

    public static <T> T bytesToObject(byte[] data){
        try{
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            T object = (T)objectInputStream.readObject();
            objectInputStream.close();
            return object;
        }catch (Exception e){
            return null;
        }
    }

}
