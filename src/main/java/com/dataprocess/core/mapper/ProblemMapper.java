package com.dataprocess.core.mapper;

import com.dataprocess.common.annotion.TableName;
import com.dataprocess.core.entity.ProblemEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:02
 * @Version: 1.0
 * @Description: problem
 */
@Repository
@Mapper
public interface ProblemMapper {

    @Select("${hql}")
    List<ProblemEntity> getProblemList(String hql) throws Exception;

    @Insert("${hql}")
    void dataInsert(String hql) throws Exception;

    @Update("${hql}")
    void dataUpdate(String hql) throws Exception;

}
