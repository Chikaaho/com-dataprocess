package com.dataprocess.core.service.impl;

import com.alibaba.fastjson2.JSON;
import com.dataprocess.common.utils.Util;

import com.dataprocess.core.ClassEnums;
import com.dataprocess.core.entity.EventProcessEntity;
import com.dataprocess.core.entity.EventReasonEntity;
import com.dataprocess.core.entity.EventTaskEntity;
import com.dataprocess.core.mapper.CronMapper;
import com.dataprocess.core.mapper.EventChildMapper;
import com.dataprocess.core.service.EventChildService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.sql.SQLSyntaxErrorException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:32
 * @Version: 1.0
 * @Description:
 */
@Service
@Slf4j
public class EventChildServiceImpl implements EventChildService {

    private EventChildMapper mapper;
    private CronMapper cm;

    @Value("${cms.configure.event-child-json}")
    private String eventFile;
    @Value("${cms.configure.dict-json}")
    private String dictFile;

    private static final Map<String, Integer> CLASSES_MAP = new HashMap<>();
    static {
        CLASSES_MAP.put(ClassEnums.PROCESS.getClassesType(), 1);
        CLASSES_MAP.put(ClassEnums.REASON.getClassesType(), 2);
        CLASSES_MAP.put(ClassEnums.TASK.getClassesType(), 3);
    }

    private final String hqlQuery = "select :column from :table where :where";
    private final String hqlInsert = "insert into :table (:column) values (:value)";
    private final String hqlUpdate = "update :table set :set where :where";

    @Override
    public void dataProcessing() {
        Map<String, Object> dataMap = Util.getMapJson(eventFile);
        if (ObjectUtils.isEmpty(dataMap)) {
            log.error("未找到event-child.json文件");
            return;
        }
        Map<String, Object> dictMap = Util.getMapJson(dictFile);
        if (ObjectUtils.isEmpty(dictMap)) {
            log.error("未找到字典映射dict.json文件");
        }
        log.info("获取到json映射,json={}", dataMap);
        try {
            dataBuild(dataMap);
        } catch (SQLSyntaxErrorException e) {
            log.error("数据同步出现错误，请检查event-child.json是否正确");
        }
    }

    private void dataBuild(Map<String, Object> map) throws SQLSyntaxErrorException {
        List<Target> targetValues = JSON.parseArray(map.get("target").toString(), Target.class);
        List<Source> sourceValues = JSON.parseArray(map.get("source").toString(), Source.class);
        //遍历target和source,取出source中目标表数据
        targetValues.forEach(target -> {
            switch (CLASSES_MAP.get(target.getTable())) {
                case (1):
                    processBuild(target, sourceValues);
                    return;
                case (2):
                    reasonBuild(target, sourceValues);
                    return;
                case (3):
                    taskBuild(target, sourceValues);
                    return;
                default:
                    log.error("");
            }

        });
    }

    private void processBuild(Target target, List<Source> sourceValues) {
        String targetTables = target.getTable();
        List<String> targetColumns = target.getColumn();
        sourceValues.forEach(source -> {
            String hql;
            //取出查询字段、表名、查询条件
            String sourceTable = source.getTable();
            List<String> where = source.getWhere();
            StringBuilder hqlCol = new StringBuilder();
            List<String> sourceColumn = source.getColumn();
            if (source.getTarget().equals(targetTables)) {
                //加入as别名
                for (int i = 0; i < sourceColumn.size(); ++i) {
                    if (i == sourceColumn.size() - 1) {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(" ");
                    } else {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(", ");
                    }
                }
                //where条件空格分割
                String hqlWhere = String.join(" ", where);
                //生成hql语句
                if (where.size() < 1) {
                    hql = hqlQuery
                            .replace(":where", "1=1")
                            .replace(":column", hqlCol.toString())
                            .replace(":table", sourceTable);
                } else {
                    hql = hqlQuery
                            .replace(":where", hqlWhere)
                            .replace(":column", hqlCol)
                            .replace(":table", sourceTable);
                }
                List<EventProcessEntity> entityList;
                try {
                    entityList = mapper.getEventProcessList(hql);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("表或字段不存在,请检查json中字段或表是否正确");
                    }
                    return;
                }
                //遍历数据结果,取出values
                entityList.forEach(entity -> {
                    List<String> values = new ArrayList<>();
                    for (Field declaredField : entity.getClass().getDeclaredFields()) {
                        declaredField.setAccessible(true);
                        String fieldName = declaredField.getName();
                        try {
                            Object fieldValues = declaredField.get(entity);
                            if (!ObjectUtils.isEmpty(fieldValues)) {
                                String finalValHql = Util.searchUserOrDept(fieldName, fieldValues.toString());
                                String userOrDept = finalValHql;
                                if (finalValHql.contains("select")) {
                                    userOrDept = cm.getUserOrDept(finalValHql);
                                }
                                values.add(userOrDept);
                            } else {
                                values.add("");
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("字段读取错误,field={}", fieldName);
                        }
                    }
                    //生成插入语句
                    String sql;
                    String sqlCol = String.join(", ", targetColumns);
                    String sqlVal = values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
                    String midHql = hqlQuery.replace(":where", "id_ = '" + entity.getId() + "'")
                            .replace(":column", sqlCol)
                            .replace(":table", targetTables);
                    String changeInsert = changeDictValues(values, targetColumns, targetTables, "insert");
                    sql = hqlInsert
                            .replace(":table", targetTables)
                            .replace(":column", sqlCol)
                            .replace(":value", changeInsert.trim().length() == 0 ? sqlVal : changeInsert);
                    try {
                        List<EventProcessEntity> entityListMid = mapper.getEventProcessList(midHql);
                        log.info("对比数据获取成功=>{}", entityListMid);
                        if (entityListMid.size() == 0 || ObjectUtils.isEmpty(entityListMid)) {
                            try {
                                mapper.dataInsert(sql);
                                log.info("数据插入成功");
                            } catch (Exception e) {
                                log.error("实体类:{}",entity);
                                log.error("表或字段不存在无法插入,请检查json中字段或表是否正确,{}", sql);
                            }
                        } else {
                            entityListMid.forEach(m -> {
                                StringBuilder updateValues = new StringBuilder();
                                for (int i = 0; i < values.size(); i++) {
                                    if (targetColumns.get(i).equals("id")) {
                                        continue;
                                    }
                                    if (i == values.size() - 1) {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("' ");
                                    } else {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("', ");
                                    }
                                }
                                String change = changeDictValues(values, targetColumns, targetTables, "update");
                                String sqlUpdate = hqlUpdate.replace(":where", "id_ = '" + entity.getId() + "'")
                                        .replace(":table", targetTables)
                                        .replace(":set", change.trim().length() == 0 ? updateValues.toString() : change);
                                try {
                                    mapper.dataUpdate(sqlUpdate);
                                    log.info("数据更新成功");
                                } catch (Exception e) {
                                    log.error("表或字段不存在无法更新数据,请检查json中字段或表是否正确,hql={}", sqlUpdate);
                                    log.error("异常详情:{}",e.toString());
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error("表或字段不存在,请检查json中字段或表是否正确, 错误信息={}",e.toString());
                    }
                });
            }
        });
    }

    private void reasonBuild(Target target, List<Source> sourceValues) {
        String targetTables = target.getTable();
        List<String> targetColumns = target.getColumn();
        sourceValues.forEach(source -> {
            String hql;
            //取出查询字段、表名、查询条件
            String sourceTable = source.getTable();
            List<String> where = source.getWhere();
            StringBuilder hqlCol = new StringBuilder();
            List<String> sourceColumn = source.getColumn();
            if (source.getTarget().equals(targetTables)) {
                //加入as别名
                for (int i = 0; i < sourceColumn.size(); ++i) {
                    if (i == sourceColumn.size() - 1) {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(" ");
                    } else {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(", ");
                    }
                }
                //where条件空格分割
                String hqlWhere = String.join(" ", where);
                //生成hql语句
                if (where.size() < 1) {
                    hql = hqlQuery
                            .replace(":where", "1=1")
                            .replace(":column", hqlCol.toString())
                            .replace(":table", sourceTable);
                } else {
                    hql = hqlQuery
                            .replace(":where", hqlWhere)
                            .replace(":column", hqlCol)
                            .replace(":table", sourceTable);
                }
                List<EventReasonEntity> entityList;
                try {
                    entityList = mapper.getEventReasonList(hql);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("表或字段不存在,请检查json中字段或表是否正确");
                    }
                    return;
                }
                //遍历数据结果,取出values
                entityList.forEach(entity -> {
                    List<String> values = new ArrayList<>();
                    for (Field declaredField : entity.getClass().getDeclaredFields()) {
                        declaredField.setAccessible(true);
                        String fieldName = declaredField.getName();
                        try {
                            Object fieldValues = declaredField.get(entity);
                            if (!ObjectUtils.isEmpty(fieldValues)) {
                                String finalValHql = Util.searchUserOrDept(fieldName, fieldValues.toString());
                                String userOrDept = finalValHql;
                                if (finalValHql.contains("select")) {
                                    userOrDept = cm.getUserOrDept(finalValHql);
                                }
                                values.add(userOrDept);
                            } else {
                                values.add("");
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("字段读取错误,field={}", fieldName);
                        }
                    }
                    //生成插入语句
                    String sql;
                    String sqlCol = String.join(", ", targetColumns);
                    String sqlVal = values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
                    String midHql = hqlQuery.replace(":where", "id_ = '" + entity.getId() + "'")
                            .replace(":column", sqlCol)
                            .replace(":table", targetTables);
                    String changeInsert = changeDictValues(values, targetColumns, targetTables, "insert");
                    sql = hqlInsert
                            .replace(":table", targetTables)
                            .replace(":column", sqlCol)
                            .replace(":value", changeInsert.trim().length() == 0 ? sqlVal : changeInsert);
                    try {
                        List<EventReasonEntity> entityListMid = mapper.getEventReasonList(midHql);
                        log.info("对比数据获取成功=>{}", entityListMid);
                        if (entityListMid.size() == 0 || ObjectUtils.isEmpty(entityListMid)) {
                            try {
                                mapper.dataInsert(sql);
                                log.info("数据插入成功");
                            } catch (Exception e) {
                                log.error("实体类:{}",entity);
                                log.error("表或字段不存在无法插入,请检查json中字段或表是否正确,{}", sql);
                            }
                        } else {
                            entityListMid.forEach(m -> {
                                StringBuilder updateValues = new StringBuilder();
                                for (int i = 0; i < values.size(); i++) {
                                    if (targetColumns.get(i).equals("id")) {
                                        continue;
                                    }
                                    if (i == values.size() - 1) {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("' ");
                                    } else {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("', ");
                                    }
                                }
                                String change = changeDictValues(values, targetColumns, targetTables, "update");
                                String sqlUpdate = hqlUpdate.replace(":where", "id_ = '" + entity.getId() + "'")
                                        .replace(":table", targetTables)
                                        .replace(":set", change.trim().length() == 0 ? updateValues.toString() : change);
                                try {
                                    mapper.dataUpdate(sqlUpdate);
                                    log.info("数据更新成功");
                                } catch (Exception e) {
                                    log.error("表或字段不存在无法更新数据,请检查json中字段或表是否正确,hql={}", sqlUpdate);
                                    log.error("异常详情:{}",e.toString());
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error("表或字段不存在,请检查json中字段或表是否正确, 错误信息={}",e.toString());
                    }
                });
            }
        });
    }

    private void taskBuild(Target target, List<Source> sourceValues) {
        String targetTables = target.getTable();
        List<String> targetColumns = target.getColumn();
        sourceValues.forEach(source -> {
            String hql;
            //取出查询字段、表名、查询条件
            String sourceTable = source.getTable();
            List<String> where = source.getWhere();
            StringBuilder hqlCol = new StringBuilder();
            List<String> sourceColumn = source.getColumn();
            if (source.getTarget().equals(targetTables)) {
                //加入as别名
                for (int i = 0; i < sourceColumn.size(); ++i) {
                    if (i == sourceColumn.size() - 1) {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(" ");
                    } else {
                        hqlCol.append(sourceColumn.get(i))
                                .append(" as ")
                                .append(targetColumns.get(i))
                                .append(", ");
                    }
                }
                //where条件空格分割
                String hqlWhere = String.join(" ", where);
                //生成hql语句
                if (where.size() < 1) {
                    hql = hqlQuery
                            .replace(":where", "1=1")
                            .replace(":column", hqlCol.toString())
                            .replace(":table", sourceTable);
                } else {
                    hql = hqlQuery
                            .replace(":where", hqlWhere)
                            .replace(":column", hqlCol)
                            .replace(":table", sourceTable);
                }
                List<EventTaskEntity> entityList;
                try {
                    entityList = mapper.getEventTaskList(hql);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("表或字段不存在,请检查json中字段或表是否正确");
                    }
                    return;
                }
                //遍历数据结果,取出values
                entityList.forEach(entity -> {
                    List<String> values = new ArrayList<>();
                    for (Field declaredField : entity.getClass().getDeclaredFields()) {
                        declaredField.setAccessible(true);
                        String fieldName = declaredField.getName();
                        try {
                            Object fieldValues = declaredField.get(entity);
                            if (!ObjectUtils.isEmpty(fieldValues)) {
                                String finalValHql = Util.searchUserOrDept(fieldName, fieldValues.toString());
                                String userOrDept = finalValHql;
                                if (finalValHql.contains("select")) {
                                    userOrDept = cm.getUserOrDept(finalValHql);
                                }
                                values.add(userOrDept);
                            } else {
                                values.add("");
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("字段读取错误,field={}", fieldName);
                        }
                    }
                    //生成插入语句
                    String sql;
                    String sqlCol = String.join(", ", targetColumns);
                    String sqlVal = values.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
                    String midHql = hqlQuery.replace(":where", "id_ = '" + entity.getId() + "'")
                            .replace(":column", sqlCol)
                            .replace(":table", targetTables);
                    System.out.println(entity);
                    String changeInsert = changeDictValues(values, targetColumns, targetTables, "insert", entity.getCreateTime());
                    sql = hqlInsert
                            .replace(":table", targetTables)
                            .replace(":column", sqlCol)
                            .replace(":value", changeInsert.trim().length() == 0 ? sqlVal : changeInsert);
                    try {
                        List<EventTaskEntity> entityListMid = mapper.getEventTaskList(midHql);
                        log.info("对比数据获取成功=>{}", entityListMid);
                        if (entityListMid.size() == 0 || ObjectUtils.isEmpty(entityListMid)) {
                            try {
                                mapper.dataInsert(sql);
                                log.info("数据插入成功");
                            } catch (Exception e) {
                                log.error("实体类:{}",entity);
                                log.error("表或字段不存在无法插入,请检查json中字段或表是否正确,{}", sql);
                            }
                        } else {
                            entityListMid.forEach(m -> {
                                StringBuilder updateValues = new StringBuilder();
                                for (int i = 0; i < values.size(); i++) {
                                    if (targetColumns.get(i).equals("id")) {
                                        continue;
                                    }
                                    if (i == values.size() - 1) {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("' ");
                                    } else {
                                        updateValues.append(targetColumns.get(i))
                                                .append("='")
                                                .append(values.get(i))
                                                .append("', ");
                                    }
                                }
                                String change = changeDictValues(values, targetColumns, targetTables, "update", entity.getCreateTime());
                                String sqlUpdate = hqlUpdate.replace(":where", "id_ = '" + entity.getId() + "'")
                                        .replace(":table", targetTables)
                                        .replace(":set", change.trim().length() == 0 ? updateValues.toString() : change);
                                try {
                                    mapper.dataUpdate(sqlUpdate);
                                    log.info("数据更新成功");
                                } catch (Exception e) {
                                    log.error("表或字段不存在无法更新数据,请检查json中字段或表是否正确,hql={}", sqlUpdate);
                                    log.error("异常详情:{}",e.toString());
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error("表或字段不存在,请检查json中字段或表是否正确, 错误信息={}",e.toString());
                    }
                });
            }
        });
    }


    private String changeDictValues(List<String> values, List<String> targetColumns, String table, String type) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> dictMap = Util.getMapJson(dictFile);
        if (ObjectUtils.isEmpty(dictMap)) {
            log.error("未找到字典映射dict.json文件");
            return "";
        }
        log.info("获取到字典json映射,json={}", dictMap);
        List<EventChildServiceImpl.Dictionary.DictData> dictValue = JSON.parseArray(dictMap.get("data").toString(), EventChildServiceImpl.Dictionary.DictData.class);
        if ("update".equals(type)) {
            out:
            for (int i = 0; i < values.size(); i++) {
                String c = targetColumns.get(i);
                String v = values.get(i);
                if (c.equals("id")) {
                    if (i == values.size() - 1) {
                        sb.append(c)
                                .append("='")
                                .append(v)
                                .append("' ");
                    } else {
                        sb.append(c)
                                .append("='")
                                .append(v)
                                .append("', ");
                    }
                    continue;
                }
                for (Dictionary.DictData dictData : dictValue) {
                    if (table.equals(dictData.getTarget())) {
                        List<Dictionary.DictData.Change> changeList = dictData.getChange();
                        for (Dictionary.DictData.Change change : changeList) {
                            String cd = change.getColumn();
                            if (cd.equals(c)) {
                                List<Dictionary.DictData.Change.Dict> dictList = change.getDict();
                                for (Dictionary.DictData.Change.Dict dict : dictList) {
                                    String key = dict.getKey();
                                    String val = dict.getVal();
                                    if (v.equals(key)) {
                                        if (i == values.size() - 1) {
                                            sb.append(c)
                                                    .append("='")
                                                    .append(val)
                                                    .append("' ");
                                        } else {
                                            sb.append(c)
                                                    .append("='")
                                                    .append(val)
                                                    .append("', ");
                                        }
                                        continue out;
                                    }
                                }
                            }
                        }
                    }
                }
                if (i == values.size() - 1) {
                    sb.append(c)
                            .append("='")
                            .append(v)
                            .append("' ");
                } else {
                    sb.append(c)
                            .append("='")
                            .append(v)
                            .append("', ");
                }
            }
        } else if ("insert".equals(type)) {
            out:
            for (int i = 0; i < values.size(); i++) {
                String c = targetColumns.get(i);
                String v = values.get(i);
                if (c.equals("id")) {
                    if (i == values.size() - 1) {
                        sb.append("'")
                                .append(v)
                                .append("' ");
                    } else {
                        sb.append("'")
                                .append(v)
                                .append("', ");
                    }
                    continue;
                }
                for (Dictionary.DictData dictData : dictValue) {
                    if (table.equals(dictData.getTarget())) {
                        List<Dictionary.DictData.Change> changeList = dictData.getChange();
                        for (Dictionary.DictData.Change change : changeList) {
                            String cd = change.getColumn();
                            if (cd.equals(c)) {
                                List<Dictionary.DictData.Change.Dict> dictList = change.getDict();
                                for (Dictionary.DictData.Change.Dict dict : dictList) {
                                    String key = dict.getKey();
                                    String val = dict.getVal();
                                    if (v.equals(key)) {
                                        if (i == values.size() - 1) {
                                            sb.append("'").append(val).append("' ");
                                        } else {
                                            sb.append("'").append(val).append("', ");
                                        }
                                        continue out;
                                    }
                                }
                            }
                        }
                    }
                }
                if (i == values.size() - 1) {
                    sb.append("'").append(v).append("' ");
                } else {
                    sb.append("'").append(v).append("', ");
                }
            }
        }
        return sb.toString();
    }

    // 更新字典值
    private String changeDictValues(List<String> values, List<String> targetColumns, String table, String type, String createTime) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> dictMap = Util.getMapJson(dictFile);
        if (ObjectUtils.isEmpty(dictMap)) {
            log.error("未找到字典映射dict.json文件");
            return "";
        }
        log.info("获取到字典json映射,json={}", dictMap);
        List<EventChildServiceImpl.Dictionary.DictData> dictValue = JSON.parseArray(dictMap.get("data").toString(), EventChildServiceImpl.Dictionary.DictData.class);
        if ("update".equals(type)) {
            out:
            for (int i = 0; i < values.size(); i++) {
                String c = targetColumns.get(i);
                String v = values.get(i);
                if (c.equals("id")) {
                    if (i == values.size() - 1) {
                        sb.append(c)
                                .append("='")
                                .append(v)
                                .append("' ");
                    } else {
                        sb.append(c)
                                .append("='")
                                .append(v)
                                .append("', ");
                    }
                    continue;
                } else if (c.equals("event_task_level")) {
                    LocalDate flagDate = LocalDate.parse(
                            "2023-04-04", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    LocalDate vDate = LocalDate.parse(
                            createTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    if (flagDate.isAfter(vDate)) {
                        if (v.equals("1")) {
                            v = "高";
                        } else if (v.equals("2")) {
                            v = "低";
                        }
                        if (i == values.size() - 1) {
                            sb.append(c)
                                    .append("='")
                                    .append(v)
                                    .append("' ");
                        } else {
                            sb.append(c)
                                    .append("='")
                                    .append(v)
                                    .append("', ");
                        }
                        log.info("日期时间已特殊处理");
                        continue;
                    }
                }
                for (Dictionary.DictData dictData : dictValue) {
                    if (table.equals(dictData.getTarget())) {
                        List<Dictionary.DictData.Change> changeList = dictData.getChange();
                        for (Dictionary.DictData.Change change : changeList) {
                            String cd = change.getColumn();
                            if (cd.equals(c)) {
                                List<Dictionary.DictData.Change.Dict> dictList = change.getDict();
                                for (Dictionary.DictData.Change.Dict dict : dictList) {
                                    String key = dict.getKey();
                                    String val = dict.getVal();
                                    if (v.equals(key)) {
                                        if (i == values.size() - 1) {
                                            sb.append(c)
                                                    .append("='")
                                                    .append(val)
                                                    .append("' ");
                                        } else {
                                            sb.append(c)
                                                    .append("='")
                                                    .append(val)
                                                    .append("', ");
                                        }
                                        continue out;
                                    }
                                }
                            }
                        }
                    }
                }
                if (i == values.size() - 1) {
                    sb.append(c)
                            .append("='")
                            .append(v)
                            .append("' ");
                } else {
                    sb.append(c)
                            .append("='")
                            .append(v)
                            .append("', ");
                }
            }
        } else if ("insert".equals(type)) {
            out:
            for (int i = 0; i < values.size(); i++) {
                String c = targetColumns.get(i);
                String v = values.get(i);
                if (c.equals("id")) {
                    if (i == values.size() - 1) {
                        sb.append("'")
                                .append(v)
                                .append("' ");
                    } else {
                        sb.append("'")
                                .append(v)
                                .append("', ");
                    }
                    continue;
                } else if (c.equals("event_task_level")) {
                    LocalDate flagDate = LocalDate.parse(
                            "2023-04-04", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    LocalDate vDate = LocalDate.parse(
                            createTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    if (flagDate.isAfter(vDate)) {
                        if (v.equals("1")) {
                            v = "高";
                        } else if (v.equals("2")) {
                            v = "低";
                        }
                        if (i == values.size() - 1) {
                            sb.append("'")
                                    .append(v)
                                    .append("' ");
                        } else {
                            sb.append("'")
                                    .append(v)
                                    .append("', ");
                        }
                        log.info("日期时间已特殊处理");
                        continue;
                    }
                }
                for (Dictionary.DictData dictData : dictValue) {
                    if (table.equals(dictData.getTarget())) {
                        List<Dictionary.DictData.Change> changeList = dictData.getChange();
                        for (Dictionary.DictData.Change change : changeList) {
                            String cd = change.getColumn();
                            if (cd.equals(c)) {
                                List<Dictionary.DictData.Change.Dict> dictList = change.getDict();
                                for (Dictionary.DictData.Change.Dict dict : dictList) {
                                    String key = dict.getKey();
                                    String val = dict.getVal();
                                    if (v.equals(key)) {
                                        if (i == values.size() - 1) {
                                            sb.append("'").append(val).append("' ");
                                        } else {
                                            sb.append("'").append(val).append("', ");
                                        }
                                        continue out;
                                    }
                                }
                            }
                        }
                    }
                }
                if (i == values.size() - 1) {
                    sb.append("'").append(v).append("' ");
                } else {
                    sb.append("'").append(v).append("', ");
                }
            }
        }
        return sb.toString();
    }

    @Autowired
    public void setMapper(EventChildMapper mapper) {
        this.mapper = mapper;
    }

    @Autowired
    public void setCm(CronMapper cm) {
        this.cm = cm;
    }

    @Data
    protected static class Source {
        private String target;
        private List<String> column;
        private String table;
        private List<String> where;
    }

    @Data
    protected static class Target {
        private String table;
        private List<String> column;
    }

    @Data
    protected static class Dictionary {
        private String data;
        private List<DictData> dictData;

        @Data
        protected static class DictData {
            private String target;
            private List<Change> change;

            @Data
            protected static class Change {
                private String column;
                private List<Dict> dict;

                @Data
                protected static class Dict {
                    private String key;
                    private String val;
                }

            }

        }

    }

}
