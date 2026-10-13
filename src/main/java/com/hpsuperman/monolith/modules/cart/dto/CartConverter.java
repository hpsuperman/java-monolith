package com.hpsuperman.monolith.modules.cart.dto;

import com.hpsuperman.monolith.modules.cart.entity.Cart;
import com.hpsuperman.monolith.modules.product.entity.Product;

import java.math.BigDecimal;

public final class CartConverter {
    private CartConverter() {}

    public static CartVO toVO(Cart cart, Product product) {
        CartVO vo = new CartVO();
        vo.setProductId(cart.getProductId());
        vo.setName(product.getName());
        vo.setMainImage(product.getMainImage());
        vo.setPrice(product.getPrice());
        vo.setQuantity(cart.getQuantity());
        vo.setAmount(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        vo.setStock(product.getStock());
        vo.setStatus(product.getStatus());
        return vo;

    }
}
