package com.dataprocess.core.controller;

import com.dataprocess.api.apiEnums.ApiStatus;
import com.dataprocess.api.request.Result;
import com.dataprocess.core.data.process.service.DataProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: hjx
 * @Date: 2023/9/5 10:43
 * @Version: 1.0
 * @Description: 手动执行调度任务
 */
@RestController("/dataProcess")
public class DataProcessController {

    private DataProcessService dataProcessService;

    @PostMapping("/start.asp")
    public Result startProcessMiddleTable() {
        try {
            dataProcessService.queryDetails();
            return Result.success(ApiStatus.SUCCESS);
        } catch (Exception e) {
            return Result.error(ApiStatus.ERROR, e.toString());
        }
    }

    @Autowired
    public void setDataProcessService(DataProcessService dataProcessService) {
        this.dataProcessService = dataProcessService;
    }
}
