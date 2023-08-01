package com;

import com.dataprocess.core.entity.EventEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * @Author: hjx
 * @Date: 2023/7/28 15:18
 * @Version: 1.0
 * @Description:
 */
public class test {

    public static void main(String[] args) throws ParseException {
        LocalDate d = LocalDate.parse("2023-04-04 12:12:12", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDate d1 = LocalDate.parse("2023-04-03", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        System.out.println(d1.isAfter(d));
    }

}
