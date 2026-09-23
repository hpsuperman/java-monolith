package com.hpsuperman.monolith.modules.material.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.material.dto.MaterialCreateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialQueryRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialUpdateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialVO;
import com.hpsuperman.monolith.modules.material.entity.Material;

public interface MaterialService extends IService<Material> {
    PageResult<MaterialVO> page(MaterialQueryRequest query);

    MaterialVO getDetail(Long id);

    MaterialVO create(MaterialCreateRequest request);

    MaterialVO update(Long id, MaterialUpdateRequest request);

    void delete(Long id);
}
