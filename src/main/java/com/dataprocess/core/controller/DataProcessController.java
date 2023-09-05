package com.dataprocess.core.controller;

import com.dataprocess.api.apiEnums.ApiStatus;
import com.dataprocess.api.request.Result;
import com.dataprocess.core.data.process.service.DataProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: hjx
 * @Date: 2023/9/5 10:43
 * @Version: 1.0
 * @Description: 手动执行调度任务
 */
@RestController("/dataProcess")
@Slf4j
public class DataProcessController {

    private DataProcessService dataProcessService;

    @PostMapping("/start.asp")
    public Result startProcessMiddleTable() {
        try {
            dataProcessService.queryDetails();
            log.info("'{}-'手工成功执行一次数据调度", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            return Result.success(ApiStatus.SUCCESS);
        } catch (Exception e) {
            log.error("手工执行调度失败,异常信息:{}",e.toString());
            return Result.error(ApiStatus.ERROR, e.toString());
        }
    }

    @Autowired
    public void setDataProcessService(DataProcessService dataProcessService) {
        this.dataProcessService = dataProcessService;
    }
}
