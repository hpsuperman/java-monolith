package com.hpsuperman.monolith.modules.cart.controller;

import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.modules.cart.dto.AddCartRequest;
import com.hpsuperman.monolith.modules.cart.dto.CartVO;
import com.hpsuperman.monolith.modules.cart.dto.UpdateCartRequest;
import com.hpsuperman.monolith.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "购物车")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {
    private final CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list(SecurityUtils.getUserId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<CartVO> add(@Valid @RequestBody AddCartRequest request) {
        return Result.success(cartService.add(SecurityUtils.getUserId(), request));
    }

    @Operation(summary = "修改数量")
    @PutMapping("/{productId}")
    public Result<Void> update(@PathVariable Long productId,
                               @Valid @RequestBody UpdateCartRequest request) {
        cartService.update(SecurityUtils.getUserId(), productId, request);
        return Result.success();
    }

    @Operation(summary = "移除一件商品")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        cartService.remove(SecurityUtils.getUserId(), productId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(SecurityUtils.getUserId());
        return Result.success();
    }
}
