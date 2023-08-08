package com.dataprocess.core.data.process.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @Author: hjx
 * @Date: 2023/8/5 21:49
 * @Version: 1.0
 * @Description:
 */
@Repository
@Mapper
public interface DataProcessMapper {

    @Select("${hql}")
    List<Map<String, Object>> getDetails(String hql);

    @Insert("${hql}")
    void insertDetails(String hql);

    @Update("${hql}")
    void updateDetails(String hql);

}
