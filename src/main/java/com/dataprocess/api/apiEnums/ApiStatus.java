package com.dataprocess.api.apiEnums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ApiStatus {

    SUCCESS (200, "操作执行成功"),
    ERROR(400, "操作执行失败")
    ;

    private Integer code;
    private String msg;

}