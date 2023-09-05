package com.dataprocess.core.task;

import com.dataprocess.core.data.process.service.DataProcessService;
import com.dataprocess.core.data.process.mapper.CronMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: hjx
 * @Date: 2023/7/24 14:59
 * @Version: 1.0
 * @Description: 定时任务控制器
 */
@Component
@EnableScheduling
@Data
@Slf4j
public class CronTabTask implements SchedulingConfigurer {

    private CronMapper cronMapper;
    private DataProcessService dataProcessService;
    @Value("${cms.id}")
    private String ID;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::dpStart,
                triggerContext -> {
                    String cron = cronMapper.getCron(ID);
                    return new CronTrigger(cron).nextExecutionTime(triggerContext);
                });
    }

    private void dpStart() {
        dataProcessService.queryDetails();
        log.info("'{}-'成功执行一次数据调度", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    @Autowired
    public void setDataProcessService(DataProcessService dataProcessService) {
        this.dataProcessService = dataProcessService;
    }

    @Autowired
    public void setCronMapper(CronMapper cronMapper) {
        this.cronMapper = cronMapper;
    }

}
