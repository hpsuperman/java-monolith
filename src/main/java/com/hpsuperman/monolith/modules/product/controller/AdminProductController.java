package com.hpsuperman.monolith.modules.product.controller;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.product.dto.AdminProductQuery;
import com.hpsuperman.monolith.modules.product.dto.AdminProductVO;
import com.hpsuperman.monolith.modules.product.dto.CreateProductRequest;
import com.hpsuperman.monolith.modules.product.dto.UpdateProductRequest;
import com.hpsuperman.monolith.modules.product.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<AdminProductVO>> list(@Valid AdminProductQuery query) {
        return Result.success(adminProductService.list(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<AdminProductVO> get(@PathVariable Long id) {
        return Result.success(adminProductService.get(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<AdminProductVO> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.success(adminProductService.create(request));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        adminProductService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return Result.success();
    }

}
