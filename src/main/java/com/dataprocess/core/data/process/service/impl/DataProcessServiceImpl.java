package com.dataprocess.core.data.process.service.impl;

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

    private static final String CONFIG_SEARCH = "select distinct source_table_name_, target_table_name_ from sys_config_map;";

    @Override
    public void queryDetails() {
        // 获取字段映射
        List<Map<String, Object>> tableQueryList = mapper.getDetails(CONFIG_SEARCH);
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
            // 字段和值中间变量
            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();
            // 遍历来源数据
            sourceDataList.forEach(dataList -> {
                // 遍历字段映射
                for (Map<String, Object> config : configMaps) {
                    String targetColumn = config.get("target_column_").toString();
                    String sourceColumn = config.get("source_column_").toString();
                    String midVal;
                    String createTime = dataList.get("CREATE_TIME_").toString();
                    // 如果值为null,toString会报空指针异常
                    try {
                        midVal = dataList.getOrDefault(sourceColumn, "null").toString();
                    } catch (NullPointerException e) {
                        midVal = "null";
                    }
                    // 更新val值,替换键值或人员部门
                    midVal = checkDict(targetColumn, midVal, targetTableValue, createTime.replace("T", " "));
                    /*if (targetTableValue.equals("event_task_middle_table")) {
                        try {
                            if (dataList.get("field00000113").toString().equals("2022102500055") && sourceColumn.equals("field00000248")) {
                                System.out.println(dataList.get("field00000076"));
                                System.out.println(midVal);
                                System.out.println(midVal.equals("null"));
                                System.exit(1);
                            }
                        }catch (Exception e) {
                            System.out.print("");
                        }
                    }*/
                    if (columns.contains(targetColumn)) {
                        if (!midVal.equals("null")) {
                            values.set(columns.indexOf(targetColumn), midVal.replace("'", "\\'"));
                        }
                    } else {
                        columns.add(targetColumn);
                        midVal = midVal.replace("'", "\\'");
                        values.add(midVal);
                    }
                }
                boolean hasData = false;
                for (Map<String, Object> map : targetDataList) {
                    if (!ObjectUtils.isEmpty(map.get("ID_"))) {
                        hasData = true;
                        break;
                    }
                }
                if (targetDataList.isEmpty() || hasData) {
                    HqlHandler insertHql = HqlHandler.insertGenerate();
                    insertHql.setTable(targetTableValue);
                    insertHql.setColumns(String.join(",", columns));
                    insertHql.setValue(values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
                    try {
                        mapper.insertDetails(insertHql.getHql());
                        //log.info("执行插入操作{}",insertHql);
                    } catch (Exception e) {
                        log.error("数据插入失败,请人工干预{}",insertHql);
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
                        log.error("数据更新失败,请人工干预{}",updateHql);
                    }
                }
            });
        });
    }

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

    private String checkDict(String key, String val, String tableName, String createTime) {
        if (val.equals("null")) {
            return val;
        }
        if (key.contains("rid") || key.contains("raid")) {
            return getUserOrDept(key, val);
        }
        if (key.contains("time")) {
            return val.replace("T", " ");
        }
        StringBuilder sb = new StringBuilder();
        HqlHandler hql = HqlHandler.selectGenerate();
        hql.setTable("sys_config_dict_map");
        hql.setCondition("target_table_name_='" + tableName + "' and target_column_='" + key + "'");
        hql.setColumns("key_,val_");
        List<Map<String, Object>> details = mapper.getDetails(hql.getHql());
        if (details.isEmpty()) {
            return val;
        }
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
