package com.usststore.search.pojo;

import lombok.Data;

import java.util.Map;

public class SearchRequest {
    private String key;
    private Integer page;
    private Map<String,String> filter;
    private static final Integer DEFAULT_SIZE=20;
    private static final Integer DEFAULT_PAGE=1;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        if(page == null){
            return DEFAULT_PAGE;
        }
        // 获取页码时做一些校验，不能小于1
        return Math.max(DEFAULT_PAGE, page);
    }
    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return DEFAULT_SIZE;
    }

    public Map<String, String> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, String> filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "key='" + key + '\'' +
                ", page=" + page +
                ", filter=" + filter +
                '}';
    }
}
