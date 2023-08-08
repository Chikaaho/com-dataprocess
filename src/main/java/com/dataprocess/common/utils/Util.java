package com.dataprocess.common.utils;

import com.alibaba.fastjson2.JSONObject;
import com.dataprocess.core.mapper.CronMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @Author: hjx
 * @Date: 2023/7/25 15:42
 * @Version: 1.0
 * @Description:
 */
@Slf4j
public class Util {

    public static Map<String, Object> getMapJson(String fileName) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            //路径
            ClassPathResource classPathResource = new ClassPathResource(fileName);
            //读取文件信息
            String str = IOUtils.toString(new InputStreamReader(classPathResource.getInputStream(),"UTF-8"));
//            String str = IOUtils.toString(new InputStreamReader(new FileInputStream(fileName)));
            //转换为Map对象
            map = JSONObject.parseObject(str, LinkedHashMap.class);
        }
        catch (Exception e) {
            log.debug("json文件未找到,json={}", fileName);
        }
        return map;
    }

    public static String searchUserOrDept(String str, String val) {
        String userStr = "Rid";
        String deptStr = "Raid";
        if (str.contains(userStr)) {
            return "select nick_name from sys_user where user_name='" + val + "'";
        } else if (str.contains(deptStr)) {
            return "select dept_name from sys_dept where dept_id='" + val + "'";
        } else {
            return val;
        }
    }

}
