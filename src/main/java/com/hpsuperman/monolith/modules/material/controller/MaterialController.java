package com.hpsuperman.monolith.modules.material.controller;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.material.dto.MaterialCreateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialQueryRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialUpdateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialVO;
import com.hpsuperman.monolith.modules.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "物料管理")
@RestController
@RequestMapping("/api/materials")
@Validated
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;

    @Operation(summary = "分页查询物料")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<PageResult<MaterialVO>> page(@Valid MaterialQueryRequest query) {
        return Result.success(materialService.page(query));
    }

    @Operation(summary = "查询物料详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<MaterialVO> detail(@PathVariable @Positive(message = "物料 ID 必须为正数") Long id) {
        return Result.success(materialService.getDetail(id));
    }

    @Operation(summary = "创建物料")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<MaterialVO> create(@Valid @RequestBody MaterialCreateRequest request) {
        return Result.success(materialService.create(request));
    }

    @Operation(summary = "更新物料")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<MaterialVO> update(@PathVariable @Positive(message = "物料 ID 必须为正数") Long id,
                                     @Valid @RequestBody MaterialUpdateRequest request) {
        return Result.success(materialService.update(id, request));
    }

    @Operation(summary = "删除物料（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<Void> delete(@PathVariable @Positive(message = "物料 ID 必须为正数") Long id) {
        materialService.delete(id);
        return Result.success();
    }
}
