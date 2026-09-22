package com.jn.common;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private long total;       // 总记录数
    private long pages;       // 总页数
    private long current;     // 当前页码
    private long size;        // 每页条数
    private List<T> records;  // 当前页数据列表

    public static <T> PageResult<T> of(long total, long pages, long current, long size, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setPages(pages);
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        pageResult.setRecords(records);
        return pageResult;
    }
}
