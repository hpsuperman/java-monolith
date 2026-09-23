package com.hpsuperman.monolith.common.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public abstract class PageQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final long MAX_PAGE_SIZE = 500;

    @Min(value = 1, message = "页码从 1 开始")
    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = MAX_PAGE_SIZE, message = "每页最多 " + MAX_PAGE_SIZE + " 条")
    @Schema(description = "每页条数", defaultValue = "10")
    private long pageSize = 10;

    public <T> Page<T> toPage() {
        return Page.of(pageNum, pageSize);
    }
}
