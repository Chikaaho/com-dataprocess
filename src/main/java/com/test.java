package com;


import lombok.Data;

import java.net.URL;
import java.text.ParseException;

import java.util.*;


/**
 * @Author: hjx
 * @Date: 2023/7/28 15:18
 * @Version: 1.0
 * @Description:
 */
public class test {

    public static void main(String[] args) throws ParseException {
        System.out.println(0x1a6f);
    }

    public static Map<String, Object> getStr(String... str) {
        Map<String, Object> map = new HashMap<>();
        if (str.length < 2) {
            map.put("load", str[0]);
        } else {
            map.put("load", Arrays.toString(str));
        }
        return map;
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
