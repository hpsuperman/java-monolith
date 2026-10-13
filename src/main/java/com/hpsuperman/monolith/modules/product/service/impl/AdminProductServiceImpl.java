package com.hpsuperman.monolith.modules.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.product.dto.*;
import com.hpsuperman.monolith.modules.product.entity.Product;
import com.hpsuperman.monolith.modules.product.mapper.ProductMapper;
import com.hpsuperman.monolith.modules.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements AdminProductService {

    @Override
    public PageResult<AdminProductVO> list(AdminProductQuery query) {
        Page<Product> page = page(query.toPage(), Wrappers.<Product>lambdaQuery()
            .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
            .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
            .and(StringUtils.hasText(query.getKeyword()), w -> w
                .like(Product::getName, query.getKeyword())
                .or()
                .like(Product::getSubtitle, query.getKeyword()))
            .orderByDesc(Product::getCreateTime)
        );
        return PageResult.of(page, ProductConverter::toAdminVO);
    }

    @Override
    public AdminProductVO get(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return ProductConverter.toAdminVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO create(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setDetail(request.getDetail());
        product.setCategoryId(request.getCategoryId());
        product.setStatus(request.getStatus() == null ? EnabledStatus.ENABLED : request.getStatus());
        product.setStock(request.getStock());
        product.setPrice(request.getPrice());
        product.setMainImage(request.getMainImage());
        save(product);
        return ProductConverter.toAdminVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateProductRequest request) {
        if (request.getName() == null && request.getSubtitle() == null
            && request.getPrice() == null && request.getMainImage() == null
            && request.getStock() == null && request.getDetail() == null
            && request.getCategoryId() == null && request.getStatus() == null) {
            throw new BizException("没有需要修改的内容");
        }
        if (getById(id) == null) {
            throw new BizException("商品不存在");
        }
        Product product = new Product();
        product.setId(id);
        BeanUtils.copyProperties(request, product);
        updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BizException("商品不存在");
        }
    }
}
