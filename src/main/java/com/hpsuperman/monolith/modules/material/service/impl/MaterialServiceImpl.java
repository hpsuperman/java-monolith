package com.hpsuperman.monolith.modules.material.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.modules.material.dto.MaterialConverter;
import com.hpsuperman.monolith.modules.material.dto.MaterialCreateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialQueryRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialUpdateRequest;
import com.hpsuperman.monolith.modules.material.dto.MaterialVO;
import com.hpsuperman.monolith.modules.material.entity.Material;
import com.hpsuperman.monolith.modules.material.mapper.MaterialMapper;
import com.hpsuperman.monolith.modules.material.service.MaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {
    @Override
    @Transactional(readOnly = true)
    public PageResult<MaterialVO> page(MaterialQueryRequest query) {
        var wrapper = Wrappers.<Material>lambdaQuery()
            .like(StringUtils.hasText(query.getCode()), Material::getCode, query.getCode())
            .like(StringUtils.hasText(query.getName()), Material::getName, query.getName())

            .eq(StringUtils.hasText(query.getCategory()), Material::getCategory, query.getCategory())
            .eq(query.getStatus() != null, Material::getStatus, query.getStatus())
            .like(StringUtils.hasText(query.getSupplier()), Material::getSupplier, query.getSupplier())

            .orderByDesc(Material::getCreateTime)
            .orderByDesc(Material::getId);

        Page<Material> page = page(query.toPage(), wrapper);
        return PageResult.of(page, MaterialConverter::toVO);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialVO getDetail(Long id) {
        Material material = getById(id);
        BizException.requireNonNull(material, "物料不存在");
        return MaterialConverter.toVO(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialVO create(MaterialCreateRequest request) {
        Material material = new Material();
        material.setCode(MaterialConverter.normalizeText(request.getCode()));
        material.setName(MaterialConverter.normalizeText(request.getName()));
        material.setSpec(MaterialConverter.normalizeText(request.getSpec()));
        material.setUnit(MaterialConverter.normalizeText(request.getUnit()));
        material.setCategory(MaterialConverter.normalizeText(request.getCategory()));
        material.setPrice(request.getPrice());
        material.setCurrency(MaterialConverter.normalizeText(request.getCurrency()));

        material.setStock(request.getStock() == null ? 0 : request.getStock());
        material.setSafetyStock(request.getSafetyStock());
        material.setSupplier(MaterialConverter.normalizeText(request.getSupplier()));
        material.setPurchaseCycle(request.getPurchaseCycle());
        material.setPurchasePrice(request.getPurchasePrice());
        material.setRemark(MaterialConverter.normalizeText(request.getRemark()));
        material.setImageUrl(MaterialConverter.normalizeText(request.getImageUrl()));

        material.setStatus(request.getStatus() == null ? EnabledStatus.ENABLED : request.getStatus());

        long existing = count(Wrappers.<Material>lambdaQuery().eq(Material::getCode, material.getCode()));
        BizException.throwIf(existing > 0, "物料编码已存在");

        boolean saved = save(material);
        BizException.throwIf(!saved, "物料创建失败");

        log.info("创建物料成功 | id={} | code={}", material.getId(), material.getCode());
        return MaterialConverter.toVO(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialVO update(Long id, MaterialUpdateRequest request) {
        Material existing = getById(id);
        BizException.requireNonNull(existing, "物料不存在");

        Material update = new Material();
        update.setId(id);

        update.setVersion(existing.getVersion());

        update.setName(requireText(request.getName(), "物料名称"));
        update.setUnit(requireText(request.getUnit(), "计量单位"));

        update.setSpec(MaterialConverter.normalizeText(request.getSpec()));
        update.setCategory(MaterialConverter.normalizeText(request.getCategory()));
        update.setSupplier(MaterialConverter.normalizeText(request.getSupplier()));
        update.setRemark(MaterialConverter.normalizeText(request.getRemark()));
        update.setImageUrl(MaterialConverter.normalizeText(request.getImageUrl()));
        update.setCurrency(MaterialConverter.normalizeText(request.getCurrency()));
        update.setStatus(request.getStatus());

        update.setPrice(request.getPrice());
        update.setSafetyStock(request.getSafetyStock());
        update.setPurchaseCycle(request.getPurchaseCycle());
        update.setPurchasePrice(request.getPurchasePrice());

        update.setStock(request.getStock());

        boolean updated = updateById(update);

        BizException.throwIf(!updated, ResultCode.DATA_CONFLICT, "数据已被他人修改，请刷新后重试");

        return MaterialConverter.toVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Material existing = getById(id);
        BizException.requireNonNull(existing, "物料不存在");

        boolean removed = removeById(id);
        BizException.throwIf(!removed, "删除失败");

        log.info("删除物料 | id={} | code={} | operator={}",
            id, existing.getCode(), SecurityUtils.getUsernameOrNull());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        BizException.throwIf(!StringUtils.hasText(trimmed), fieldName + "不能清空");
        return trimmed;
    }
}
