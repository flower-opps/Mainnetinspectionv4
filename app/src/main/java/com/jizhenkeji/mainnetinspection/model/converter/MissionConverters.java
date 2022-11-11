package com.jizhenkeji.mainnetinspection.model.converter;

import androidx.room.TypeConverter;

import com.jizhenkeji.mainnetinspection.common.Location;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

public class MissionConverters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Location bytesToLocation(byte[] objectBytes){
        return bytesToObject(objectBytes);
    }

    @TypeConverter
    public static byte[] locationToBytes(Location location){
        return objectToBytes(location);
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
