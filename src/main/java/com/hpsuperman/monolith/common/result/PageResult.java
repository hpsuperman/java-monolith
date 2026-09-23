package com.hpsuperman.monolith.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Getter
@ToString
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long pageNum;

    private final long pageSize;

    private final long total;

    private final long pages;

    private final List<T> records;

    private PageResult(long pageNum, long pageSize, long total, long pages, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pages;
        this.records = records;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), page.getRecords());
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> list = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), list);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(pageNum, pageSize, 0L, 0L, Collections.emptyList());
    }
}
