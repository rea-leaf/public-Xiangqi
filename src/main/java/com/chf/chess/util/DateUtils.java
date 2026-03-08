package com.chf.chess.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DateUtils 类。
 * 通用工具类。
 */
public class DateUtils {

    public static String getDateTimeString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return dateFormat.format(date);
    }
}
