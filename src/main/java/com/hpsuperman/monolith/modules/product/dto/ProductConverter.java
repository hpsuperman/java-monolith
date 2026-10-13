package com.hpsuperman.monolith.modules.product.dto;

import com.hpsuperman.monolith.modules.product.entity.Product;

public final class ProductConverter {
    private ProductConverter() {
    }

    public static AdminProductVO toAdminVO(Product product) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setMainImage(product.getMainImage());
        vo.setDetail(product.getDetail());
        vo.setCategoryId(product.getCategoryId());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    public static ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setMainImage(product.getMainImage());
        vo.setDetail(product.getDetail());
        vo.setCategoryId(product.getCategoryId());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }
}
