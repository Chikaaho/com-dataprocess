package com.dataprocess.core.mapper;

import com.dataprocess.core.entity.EventEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:02
 * @Version: 1.0
 * @Description: problem
 */
@Repository
@Mapper
public interface EventMapper {

    @Select("${select}")
    List<EventEntity> getEventList(String select);

    @Insert("${hql}")
    void dataInsert(String hql) throws Exception;

    @Update("${hql}")
    void dataUpdate(String hql) throws Exception;

}
