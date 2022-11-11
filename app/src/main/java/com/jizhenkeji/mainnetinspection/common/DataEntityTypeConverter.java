package com.jizhenkeji.mainnetinspection.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataEntityTypeConverter {

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String dateToString(Date date){
        return df.format(date);
    }

}
