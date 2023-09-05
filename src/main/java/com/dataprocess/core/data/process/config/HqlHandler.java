package com.dataprocess.core.data.process.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: hjx
 * @Date: 2023/8/5 23:22
 * @Version: 1.0
 * @Description:
 */
@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class HqlHandler {

    private String hql;

    /*
    * 更新默认查询sql语句
    * */
    public String checkQueryHql() {
        String hqlImpl = this.getHql();
        if (hqlImpl.contains("{columns}")) {
            hqlImpl = hqlImpl.replace("{columns}", "*");
        }
        if (hqlImpl.contains("{condition}")) {
            hqlImpl = hqlImpl.replace("{condition}", "1=1");
        }
        if (hqlImpl.contains("{table}")) {
            if (log.isDebugEnabled()) {
                log.debug("未填写查询表名,请检查语句={}",hqlImpl);
            }
            return "";
        }
        this.setHql(hqlImpl);
        return hqlImpl;
    }

    public String checkInsertHql() {
        String hqlImpl = this.getHql();
        if (hqlImpl.contains("{table}")) {
            if (log.isDebugEnabled()) {
                log.debug("未填写查询表名,请检查语句={}",hqlImpl);
            }
            return "";
        }
        this.setHql(hqlImpl);
        return hqlImpl;
    }

    public String checkUpdateHql() {
        String hqlImpl = this.getHql();
        if (hqlImpl.contains("{table}")) {
            if (log.isDebugEnabled()) {
                log.debug("未填写查询表名,请检查语句={}",hqlImpl);
            }
            return "";
        }
        this.setHql(hqlImpl);
        return hqlImpl;
    }

    public void setColumns(String changeTo) {
        String h = this.getHql();
        String replace = h.replace("{columns}", changeTo);
        this.setHql(replace);
    }

    public void setTable(String changeTo) {
        String h = this.getHql();
        String replace = h.replace("{table}", changeTo);
        this.setHql(replace);
    }

    public void setCondition(String changeTo) {
        String h = this.getHql();
        String replace = h.replace("{condition}", changeTo);
        this.setHql(replace);
    }

    public void setValue(String changeTo) {
        String h = this.getHql();
        String replace = h.replace("{value}", changeTo);
        this.setHql(replace);
    }

    public void setChange(String changeTo) {
        String h = this.getHql();
        String replace = h.replace("{changes}", changeTo);
        this.setHql(replace);
    }

    public static HqlHandler selectGenerate() {
        return new HqlHandler("select {columns} from {table} where {condition}");
    }

    public static HqlHandler insertGenerate() {
        return new HqlHandler("insert into {table} ({columns}) values ({value})");
    }

    public static HqlHandler updateGenerate() {
        return new HqlHandler("update {table} set {changes} where {condition}");
    }

    @Override
    public String toString() {
        return "'" + hql + "'";
    }
}
