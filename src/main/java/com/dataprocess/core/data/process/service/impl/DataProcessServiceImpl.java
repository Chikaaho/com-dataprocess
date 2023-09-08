package com.dataprocess.core.data.process.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.dataprocess.common.utils.Util;
import com.dataprocess.core.data.process.config.HqlHandler;
import com.dataprocess.core.data.process.mapper.DataProcessMapper;
import com.dataprocess.core.data.process.service.DataProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: hjx
 * @Date: 2023/8/5 21:53
 * @Version: 1.0
 * @Description:
 */
@Service
@Slf4j
public class DataProcessServiceImpl implements DataProcessService {

    private DataProcessMapper mapper;

    //配置查询,取出去重后的来源和目标表名
    private static final String CONFIG_SEARCH = "select distinct source_table_name_, target_table_name_ from sys_config_map;";

    /*
    * 主业务入口，获取源和目标数据做增量更新
    * */
    @Override
    public synchronized void queryDetails() {
        // 获取表映射
        List<Map<String, Object>> tableQueryList = mapper.getDetails(CONFIG_SEARCH);
        //遍历表映射
        tableQueryList.forEach(tableMap -> {
            // 获取来源和目标表名
            String sourceTableValue = tableMap.get("source_table_name_").toString();
            String targetTableValue = tableMap.get("target_table_name_").toString();
            // 根据表名查询到字段映射集合
            HqlHandler configHql = HqlHandler.selectGenerate();
            configHql.setTable("sys_config_map");
            configHql.setColumns("source_column_,target_column_");
            configHql.setCondition("source_table_name_ ='"
                    + sourceTableValue
                    + "' and target_table_name_='"
                    + targetTableValue + "'");
            // 获取到一个表调度的来源\目标表名及字段
            List<Map<String, Object>> configMaps = mapper.getDetails(configHql.getHql());
            HqlHandler targetHql = HqlHandler.selectGenerate();
            targetHql.setTable(targetTableValue);
            targetHql.checkQueryHql();
            HqlHandler sourceHql = HqlHandler.selectGenerate();
            sourceHql.setTable(sourceTableValue);
            sourceHql.checkQueryHql();
            // 获取目标表和源表数据
            List<Map<String, Object>> targetDataList = mapper.getDetails(targetHql.getHql());
            List<Map<String, Object>> sourceDataList = mapper.getDetails(sourceHql.getHql());
            // 遍历来源数据
            sourceDataList.forEach(dataList -> {
                // 字段和值中间变量
                List<String> columns = new ArrayList<>();
                List<String> values = new ArrayList<>();
                // 遍历字段映射
                for (Map<String, Object> config : configMaps) {
                    String targetColumn = config.get("target_column_").toString();
                    String sourceColumn = config.get("source_column_").toString();
                    String midVal;
                    String createTime = dataList.getOrDefault("CREATE_TIME_", "2023").toString();
                    // 如果取出来的字段是url,拼id
                    if (Util.isUrl(sourceColumn)) {
                        midVal = sourceColumn + dataList.get("ID_");
                    } else {
                        // 如果值为null,toString会报空指针异常
                        try {
                            midVal = dataList.getOrDefault(sourceColumn, "null").toString();
                        } catch (NullPointerException e) {
                            midVal = "null";
                        }
                    }
                    // 更新val值,替换键值、人员部门、url，去除mybatis查询日期时间中的'T'
                    midVal = checkDict(targetColumn, midVal, targetTableValue, createTime.replace("T", " "));
                    if (columns.contains(targetColumn)) {
                        if (!midVal.equals("null")) {
                            values.set(columns.indexOf(targetColumn), midVal.replace("'", "\\'"));
                        }
                    } else {
                        columns.add(targetColumn);
                        midVal = midVal.replace("'", "\\'");
                        if (StringUtils.isEmpty(midVal)) {
                            midVal = "null";
                        }
                        values.add(midVal);
                    }
                }
                boolean hasData = false;
                for (Map<String, Object> map : targetDataList) {
                    String sourceId = dataList.get("ID_").toString();
                    String targetId = map.get("id_").toString();
                    if (sourceId.equals(targetId)) {
                        hasData = true;
                        break;
                    }
                }
                if (!hasData) {
                    HqlHandler insertHql = HqlHandler.insertGenerate();
                    insertHql.setTable(targetTableValue);
                    insertHql.setColumns(String.join(",", columns));
                    insertHql.setValue(values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
                    try {
                        mapper.insertDetails(insertHql.getHql());
                        //log.info("执行插入操作{}",insertHql);
                    } catch (Exception e) {
                        log.error("数据插入失败,请人工干预{}", insertHql);
                    }
                } else {
                    HqlHandler updateHql = HqlHandler.updateGenerate();
                    updateHql.setTable(targetTableValue);
                    updateHql.setChange(updateChanges(columns, values));
                    updateHql.setCondition("ID_='" + dataList.get("ID_") + "'");
                    try {
                        mapper.updateDetails(updateHql.getHql());
                        //log.info("执行更新操作{}",updateHql);
                    } catch (Exception e) {
                        log.error("数据更新失败,请人工干预{}", updateHql);
                    }
                }
            });
        });
    }

    /*
    * 更新用户部门数据
    * */
    private String getUserOrDept(String key, String val) {
        String userStr = "rid";
        String deptStr = "raid";
        StringBuilder sb = new StringBuilder();
        if (key.contains(userStr)) {
            if (val.contains(",")) {
                String[] valSplit = val.split(",");
                for (String s : valSplit) {
                    HqlHandler hql = HqlHandler.selectGenerate();
                    hql.setColumns("nick_name");
                    hql.setTable("sys_user");
                    hql.setCondition("user_name='" + s + "'");
                    List<Map<String, Object>> userList = mapper.getDetails(hql.getHql());
                    try {
                        sb.append(userList.get(0).entrySet().iterator().next().getValue());
                    } catch (Exception e) {
                        sb.append("");
                    }
                    sb.append(",");
                }
                return sb.substring(0, sb.length() - 1);
            } else {
                HqlHandler hql = HqlHandler.selectGenerate();
                hql.setColumns("nick_name");
                hql.setTable("sys_user");
                hql.setCondition("user_name='" + val + "'");
                List<Map<String, Object>> userList = mapper.getDetails(hql.getHql());
                try {
                    sb.append(userList.get(0).entrySet().iterator().next().getValue());
                } catch (Exception e) {
                    sb.append("");
                }
                return sb.toString();
            }
        } else if (key.contains(deptStr)) {
            if (val.contains(",")) {
                String[] valSplit = val.split(",");
                for (String s : valSplit) {
                    HqlHandler hql = HqlHandler.selectGenerate();
                    hql.setColumns("dept_name");
                    hql.setTable("sys_dept");
                    hql.setCondition("dept_id='" + s + "'");
                    List<Map<String, Object>> deptList = mapper.getDetails(hql.getHql());
                    try {
                        sb.append(deptList.get(0).entrySet().iterator().next().getValue());
                    } catch (Exception e) {
                        sb.append("");
                    }
                    sb.append(",");
                }
                return sb.substring(0, sb.length() - 1);
            } else {
                HqlHandler hql = HqlHandler.selectGenerate();
                hql.setColumns("dept_name");
                hql.setTable("sys_dept");
                hql.setCondition("dept_id='" + val + "'");
                List<Map<String, Object>> deptList = mapper.getDetails(hql.getHql());
                try {
                    sb.append(deptList.get(0).entrySet().iterator().next().getValue());
                } catch (Exception e) {
                    sb.append("");
                }
                return sb.toString();
            }
        } else {
            return val;
        }
    }

    /*
    * 更新字典数据
    * */
    private String checkDict(String key, String val, String tableName, String createTime) {
        if (val.equals("null")) {
            return val;
        }
        // 开发约束，字段中包含'rid'时需要将用户登录名替换为人员中文名称，包含'raid'时需替换部门id为部门名称
        if (key.contains("rid") || key.contains("raid")) {
            return getUserOrDept(key, val);
        }
        if (key.contains("time")) {
            return val.replace("T", " ");
        }
        StringBuilder sb = new StringBuilder();
        //查询字典映射
        HqlHandler hql = HqlHandler.selectGenerate();
        hql.setTable("sys_config_dict_map");
        hql.setCondition("target_table_name_='" + tableName + "' and target_column_='" + key + "'");
        hql.setColumns("key_,val_");
        List<Map<String, Object>> details = mapper.getDetails(hql.getHql());
        if (details.isEmpty()) {
            return val;
        }
        //对于两个特殊值做单独处理
        if (key.equals("problem_priority") || key.equals("event_task_level")) {
            LocalDate flagDate = LocalDate.parse(
                    "2023-04-04", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate vDate = LocalDate.parse(
                    createTime.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (flagDate.isAfter(vDate)) {
                if (val.equals("1")) {
                    return "高";
                } else if (val.equals("2")) {
                    return "低";
                }
            }
        }
        try {
            for (Map<String, Object> detail : details) {
                if (detail.get("key_").equals(val)) {
                    sb.append(detail.get("val_"));
                }
            }
        } catch (Exception e) {
            sb.append("");
        }
        return sb.toString();
    }

    /*
    * 将字段和值临时列表转换为更新结果
    * */
    private String updateChanges(List<String> columns, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            String v = values.get(i);
            sb.append(columns.get(i))
                    .append("='")
                    .append(v)
                    .append("',");
        }
        return sb.substring(0, sb.length() - 1);
    }

    @Autowired
    public void setMapper(DataProcessMapper mapper) {
        this.mapper = mapper;
    }
}
