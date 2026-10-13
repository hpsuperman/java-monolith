package com.hpsuperman.monolith.modules.product.service;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.product.dto.AdminProductQuery;
import com.hpsuperman.monolith.modules.product.dto.AdminProductVO;
import com.hpsuperman.monolith.modules.product.dto.CreateProductRequest;
import com.hpsuperman.monolith.modules.product.dto.UpdateProductRequest;

public interface AdminProductService {
    PageResult<AdminProductVO> list(AdminProductQuery query);

    AdminProductVO get(Long id);

    AdminProductVO create(CreateProductRequest request);

    void update(Long id, UpdateProductRequest request);

    void delete(Long id);
}
