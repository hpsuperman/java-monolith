package com.hpsuperman.monolith.modules.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.product.dto.ProductConverter;
import com.hpsuperman.monolith.modules.product.dto.ProductQuery;
import com.hpsuperman.monolith.modules.product.dto.ProductVO;
import com.hpsuperman.monolith.modules.product.entity.Product;
import com.hpsuperman.monolith.modules.product.mapper.ProductMapper;
import com.hpsuperman.monolith.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    @Override
    public PageResult<ProductVO> list(ProductQuery query) {
        Page<Product> page = page(query.toPage(), Wrappers.<Product>lambdaQuery()
            .eq(Product::getStatus, EnabledStatus.ENABLED)
            .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
            .and(StringUtils.hasText(query.getKeyword()), w -> w
                .like(Product::getName, query.getKeyword())
                .or()
                .like(Product::getSubtitle, query.getKeyword()))
            .orderByDesc(Product::getCreateTime)
        );
        return PageResult.of(page, ProductConverter::toVO);
    }

    @Override
    public ProductVO get(Long id) {
        Product product = getById(id);
        if (product == null || !EnabledStatus.ENABLED.equals(product.getStatus())) {
            throw new BizException("查询不到商品");
        }
        return ProductConverter.toVO(product);
    }
}
