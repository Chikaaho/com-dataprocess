package com.dataprocess.api.request;

import com.dataprocess.api.apiEnums.ApiStatus;
import lombok.*;

/**
 * @Author: hjx
 * @Date: 2023/9/5 10:48
 * @Version: 1.0
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {

    private Integer code;
    private String msg;

    public static Result success(ApiStatus apiStatus) {
        Result result = new Result();
        result.setMsg(apiStatus.getMsg());
        result.setCode(apiStatus.getCode());
        return result;
    }

    public static Result success(ApiStatus apiStatus, String msg) {
        Result result = new Result();
        result.setMsg(msg);
        result.setCode(apiStatus.getCode());
        return result;
    }

    public static Result success(String msg) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg(msg);
        return result;
    }

    public static Result success(Integer code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static Result success() {
        Result result = new Result();
        result.setCode(ApiStatus.SUCCESS.getCode());
        result.setMsg(ApiStatus.SUCCESS.getMsg());
        return result;
    }

    public static Result error(ApiStatus apiStatus) {
        Result result = new Result();
        result.setMsg(apiStatus.getMsg());
        result.setCode(apiStatus.getCode());
        return result;
    }

    public static Result error(ApiStatus apiStatus, String msg) {
        Result result = new Result();
        result.setMsg(msg);
        result.setCode(apiStatus.getCode());
        return result;
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.setCode(400);
        result.setMsg(msg);
        return result;
    }

    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.setCode(ApiStatus.ERROR.getCode());
        result.setMsg(ApiStatus.ERROR.getMsg());
        return result;
    }

    public static Result error() {
        Result result = new Result();
        result.setCode(400);
        result.setMsg("执行操作失败");
        return result;
    }



}
