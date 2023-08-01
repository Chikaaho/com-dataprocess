package com.dataprocess.core.service.impl;

import com.alibaba.fastjson2.JSON;
import com.dataprocess.common.utils.Util;
import com.dataprocess.core.entity.ProblemEntity;
import com.dataprocess.core.mapper.CronMapper;
import com.dataprocess.core.mapper.ProblemMapper;
import com.dataprocess.core.service.ProblemService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


import java.lang.reflect.Field;
import java.sql.SQLSyntaxErrorException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:32
 * @Version: 1.0
 * @Description:
 */
@Slf4j
@Service
@Transactional
public class ProblemServiceImpl implements ProblemService {

    private ProblemMapper mapper;
    private CronMapper cm;

    @Value("${cms.configure.problem-json}")
    private String problemFile;
    @Value("${cms.configure.dict-json}")
    private String dictFile;

    @Override
    public void dataProcessing() {
        Map<String, Object> dataMap = Util.getMapJson(problemFile);
        if (ObjectUtils.isEmpty(dataMap)) {
            log.error("未找到problem.json文件");
            return;
        }
        log.info("获取到字段json映射,json={}", dataMap);
        try {
            dataBuild(dataMap);
        } catch (SQLSyntaxErrorException e) {
            log.error("数据同步出现错误，请检查problem.json是否正确");
        }
    }

    private void dataBuild(Map<String, Object> map) throws SQLSyntaxErrorException {
        List<Target> targetValues = JSON.parseArray(map.get("target").toString(), Target.class);
        List<Source> sourceValues = JSON.parseArray(map.get("source").toString(), Source.class);
        String hqlQuery = "select :column from :table where :where";
        String hqlInsert = "insert into :table (:column) values (:value)";
        String hqlUpdate = "update :table set :set where :where";
        //遍历target和source,取出source中目标表数据
        targetValues.forEach(target -> {
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
                    List<ProblemEntity> problemList;
                    try {
                        problemList = mapper.getProblemList(hql);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("表或字段不存在,请检查json中字段或表是否正确");
                        }
                        return;
                    }
                    //遍历数据结果,取出values
                    problemList.forEach(entity -> {
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
                        String midHql = hqlQuery.replace(":where", "problem_code = '" + entity.getProblemCode() + "'")
                                .replace(":column", sqlCol)
                                .replace(":table", targetTables);
                        String changeInsert = changeDictValues(values, targetColumns, targetTables, "insert", entity.getCreateTime());
                        sql = hqlInsert
                                .replace(":table", targetTables)
                                .replace(":column", sqlCol)
                                .replace(":value", changeInsert.trim().isEmpty() ? sqlVal : changeInsert);
                        try {
                            List<ProblemEntity> problemListMid = mapper.getProblemList(midHql);
                            log.info("对比数据获取成功=>{}", problemListMid);
                            if (problemListMid.isEmpty() || ObjectUtils.isEmpty(problemListMid)) {
                                try {
                                    mapper.dataInsert(sql);
                                    log.info("数据插入成功");
                                } catch (Exception e) {
                                    log.error("实体类:{}", entity);
                                    log.error("表或字段不存在无法插入,请检查json中字段或表是否正确,{}", sql);
                                }
                            } else {
                                problemListMid.forEach(m -> {
                                    StringBuilder updateValues = new StringBuilder();
                                    for (int i = 0; i < values.size(); i++) {
                                        if (targetColumns.get(i).equals("problem_code")) {
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
                                    String sqlUpdate = hqlUpdate.replace(":where", "problem_code = '" + entity.getProblemCode() + "'")
                                            .replace(":table", targetTables)
                                            .replace(":set", change.trim().isEmpty() ? updateValues.toString() : change);
                                    try {
                                        mapper.dataUpdate(sqlUpdate);
                                        log.info("数据更新成功");
                                    } catch (Exception e) {
                                        log.error("表或字段不存在无法更新数据,请检查json中字段或表是否正确,hql={}", sqlUpdate);
                                        log.error("异常详情:{}", e.toString());
                                    }
                                });
                            }
                        } catch (Exception e) {
                            log.error("表或字段不存在,请检查json中字段或表是否正确, 错误信息={}", e.toString());
                        }
                    });
                }
            });
        });
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
        List<Dictionary.DictData> dictValue = JSON.parseArray(dictMap.get("data").toString(), Dictionary.DictData.class);
        if ("update".equals(type)) {
            out:
            for (int i = 0; i < values.size(); i++) {
                String c = targetColumns.get(i);
                String v = values.get(i);
                if (c.equals("problem_code")) {
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
                } else if (c.equals("problem_priority")) {
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
                if (c.equals("problem_code")) {
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
                } else if (c.equals("problem_priority")) {
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
                for (int j = 0; j < dictValue.size(); j++) {
                    Dictionary.DictData dictData = dictValue.get(j);
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
    public void setMapper(ProblemMapper mapper) {
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


