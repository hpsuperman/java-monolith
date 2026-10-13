package com.hpsuperman.monolith.modules.cart.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.modules.cart.dto.AddCartRequest;
import com.hpsuperman.monolith.modules.cart.dto.CartConverter;
import com.hpsuperman.monolith.modules.cart.dto.CartVO;
import com.hpsuperman.monolith.modules.cart.dto.UpdateCartRequest;
import com.hpsuperman.monolith.modules.cart.entity.Cart;
import com.hpsuperman.monolith.modules.cart.mapper.CartMapper;
import com.hpsuperman.monolith.modules.cart.service.CartService;
import com.hpsuperman.monolith.modules.product.entity.Product;
import com.hpsuperman.monolith.modules.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {
    private static final int MAX_QUANTITY = 99;
    private final ProductMapper productMapper;

    @Override
    public List<CartVO> list(Long userId) {
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, userId).orderByDesc(Cart::getCreateTime).list();
        if (carts == null || carts.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = carts.stream().map(Cart::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectByIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<CartVO> result = new ArrayList<>();
        for (Cart cart : carts) {
            Product product = productMap.get(cart.getProductId());
            if (product == null) {
                continue;
            }
            result.add(CartConverter.toVO(cart, product));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO add(Long userId, AddCartRequest request) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || !EnabledStatus.ENABLED.equals(product.getStatus())) {
            throw new BizException("商品不存在或已下架");
        }
        Cart cart = lambdaQuery().eq(Cart::getUserId, userId).eq(Cart::getProductId, product.getId()).one();
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setQuantity(request.getQuantity());
            save(cart);
        } else {
            int total = cart.getQuantity() + request.getQuantity();
            if (total > MAX_QUANTITY) {
                throw new BizException("单个商品最多" + MAX_QUANTITY + "件");
            }
            cart.setQuantity(total);
            updateById(cart);
        }

        return CartConverter.toVO(cart, product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long productId, UpdateCartRequest request) {
        boolean updated = lambdaUpdate()
            .eq(Cart::getUserId, userId)
            .eq(Cart::getProductId, productId)
            .set(Cart::getQuantity, request.getQuantity())
            .update();
        if (!updated) {
            throw new BizException("购物车里没有这件商品");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long productId) {
        boolean removed = lambdaUpdate()
            .eq(Cart::getUserId, userId)
            .eq(Cart::getProductId, productId)
            .remove();
        if (!removed) {
            throw new BizException("购物车里没有这件商品");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        lambdaUpdate().eq(Cart::getUserId, userId).remove();

    }
}
