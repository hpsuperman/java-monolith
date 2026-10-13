package com.hpsuperman.monolith.modules.product.service;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.product.dto.ProductQuery;
import com.hpsuperman.monolith.modules.product.dto.ProductVO;

public interface ProductService {
    PageResult<ProductVO> list(ProductQuery query);

    ProductVO get(Long id);
}
