package com;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dataprocess.common.utils.Util;
import com.dataprocess.core.data.process.config.HqlHandler;
import com.dataprocess.core.service.impl.ProblemServiceImpl;
import lombok.Data;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: hjx
 * @Date: 2023/7/28 15:18
 * @Version: 1.0
 * @Description:
 */
public class test {

    public static void main(String[] args) throws ParseException {
        String str = "null";
        System.out.println(str.equals("null"));
    }

    @Data
    protected static class Dictionary {
        private String data;
        private List<DictData> dictData;

        @Data
        protected static class DictData {
            private String target;
            private List<Change> change;

            @Data
            protected static class Change {
                private String column;
                private List<Dict> dict;

                @Data
                protected static class Dict {
                    private String key;
                    private String val;
                }

            }

        }

    }


}
