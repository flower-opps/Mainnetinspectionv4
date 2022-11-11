package com.jizhenkeji.mainnetinspection.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MissionEntityTypeConverter {

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public static String dateToString(Date date){
        return df.format(date);
    }

}
