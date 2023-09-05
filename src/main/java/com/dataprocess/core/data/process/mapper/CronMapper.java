package com.dataprocess.core.data.process.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:14
 * @Version: 1.0
 * @Description: get cron time
 */
@Mapper
@Repository
public interface CronMapper {

    @Select("select cron_ from cron_table where id_=#{id}")
    String getCron(String id);

    @Select("${hql}")
    String getUserOrDept(String hql);

}
