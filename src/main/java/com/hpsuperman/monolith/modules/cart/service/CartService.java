package com.hpsuperman.monolith.modules.cart.service;

import com.hpsuperman.monolith.modules.cart.dto.AddCartRequest;
import com.hpsuperman.monolith.modules.cart.dto.CartVO;
import com.hpsuperman.monolith.modules.cart.dto.UpdateCartRequest;

import java.util.List;

public interface CartService {
    List<CartVO> list(Long userId);

    CartVO add(Long userId, AddCartRequest request);

    void update(Long userId, Long productId, UpdateCartRequest request);

    void remove(Long userId, Long productId);

    void clear(Long userId);
}
