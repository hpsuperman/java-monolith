package com.hpsuperman.monolith.modules.product.controller;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.product.dto.ProductQuery;
import com.hpsuperman.monolith.modules.product.dto.ProductVO;
import com.hpsuperman.monolith.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid ProductQuery query) {
        return Result.success(productService.list(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> get(@PathVariable Long id) {
        return Result.success(productService.get(id));
    }
}
