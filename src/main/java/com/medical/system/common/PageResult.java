package com.medical.system.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages(size == 0 ? 0 : (int) Math.ceil((double) total / size));
        return result;
    }
}
