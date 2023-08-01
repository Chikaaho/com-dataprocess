package com.dataprocess.core;

/**
 * @Author: hjx
 * @Date: 2023/7/25 16:43
 * @Version: 1.0
 * @Description:
 */
public enum ClassEnums {

    PROCESS("event_process_middle_table"),
    REASON("event_reason_middle_table"),
    TASK("event_task_middle_table");

    private final String classesType;

    ClassEnums(String classesType) {
        this.classesType = classesType;
    }

    public String getClassesType() {
        return classesType;
    }
}
