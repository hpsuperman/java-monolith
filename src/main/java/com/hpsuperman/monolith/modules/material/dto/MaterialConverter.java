package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.modules.material.entity.Material;

public final class MaterialConverter {
    private MaterialConverter() {
    }

    public static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    public static MaterialVO toVO(Material material) {
        if (material == null) {
            return null;
        }
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setCode(material.getCode());
        vo.setName(material.getName());
        vo.setSpec(material.getSpec());
        vo.setUnit(material.getUnit());
        vo.setCategory(material.getCategory());
        vo.setStatus(material.getStatus());
        vo.setPrice(material.getPrice());
        vo.setCurrency(material.getCurrency());
        vo.setStock(material.getStock());
        vo.setSafetyStock(material.getSafetyStock());
        vo.setSupplier(material.getSupplier());
        vo.setPurchaseCycle(material.getPurchaseCycle());
        vo.setPurchasePrice(material.getPurchasePrice());
        vo.setRemark(material.getRemark());
        vo.setImageUrl(material.getImageUrl());
        vo.setCreateTime(material.getCreateTime());
        vo.setUpdateTime(material.getUpdateTime());
        return vo;
    }
}
