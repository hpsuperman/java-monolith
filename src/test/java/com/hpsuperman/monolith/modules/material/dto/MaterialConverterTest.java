package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.modules.material.entity.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaterialConverter")
class MaterialConverterTest {
    @Nested
    @DisplayName("normalizeText")
    class NormalizeText {
        @Test
        @DisplayName("null 必须原样返回 null——一旦归一成空串，「不修改」就变成了「清空」")
        void keepsNullAsNull() {
            assertThat(MaterialConverter.normalizeText(null)).isNull();
        }

        @Test
        @DisplayName("空串与全空白都归一成空串，即「清空」")
        void treatsBlankAsClear() {
            assertThat(MaterialConverter.normalizeText("")).isEmpty();
            assertThat(MaterialConverter.normalizeText("   ")).isEmpty();

            assertThat(MaterialConverter.normalizeText("\t\n ")).isEmpty();
        }

        @Test
        @DisplayName("正常值去掉首尾空白")
        void trimsNormalValue() {
            assertThat(MaterialConverter.normalizeText("  备注 ")).isEqualTo("备注");
            assertThat(MaterialConverter.normalizeText("备注")).isEqualTo("备注");
        }
    }

    @Test
    @DisplayName("toVO 逐字段映射，null 进 null 出")
    void toVOMapsEveryField() {
        Material material = new Material();
        material.setId(1L);
        material.setCode("M-001");
        material.setName("六角螺栓");
        material.setSpec("M8x40");
        material.setUnit("个");
        material.setCategory("紧固件");
        material.setStatus(EnabledStatus.ENABLED);
        material.setPrice(new BigDecimal("0.5000"));
        material.setCurrency("CNY");
        material.setStock(100);
        material.setSafetyStock(20);
        material.setSupplier("某某五金");
        material.setPurchaseCycle(7);
        material.setPurchasePrice(new BigDecimal("0.3000"));
        material.setRemark("备注");
        material.setImageUrl("https://example.com/a.png");
        LocalDateTime now = LocalDateTime.now();
        material.setCreateTime(now);
        material.setUpdateTime(now);

        MaterialVO vo = MaterialConverter.toVO(material);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getCode()).isEqualTo("M-001");
        assertThat(vo.getName()).isEqualTo("六角螺栓");
        assertThat(vo.getSpec()).isEqualTo("M8x40");
        assertThat(vo.getUnit()).isEqualTo("个");
        assertThat(vo.getCategory()).isEqualTo("紧固件");
        assertThat(vo.getStatus()).isEqualTo(EnabledStatus.ENABLED);

        assertThat(vo.getPrice()).isEqualByComparingTo("0.5000");
        assertThat(vo.getCurrency()).isEqualTo("CNY");
        assertThat(vo.getStock()).isEqualTo(100);
        assertThat(vo.getSafetyStock()).isEqualTo(20);
        assertThat(vo.getSupplier()).isEqualTo("某某五金");
        assertThat(vo.getPurchaseCycle()).isEqualTo(7);
        assertThat(vo.getPurchasePrice()).isEqualByComparingTo("0.3000");
        assertThat(vo.getRemark()).isEqualTo("备注");
        assertThat(vo.getImageUrl()).isEqualTo("https://example.com/a.png");
        assertThat(vo.getCreateTime()).isEqualTo(now);
        assertThat(vo.getUpdateTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("toVO 的 null 安全：实体为 null 时返回 null 而不是抛异常")
    void toVOIsNullSafe() {
        assertThat(MaterialConverter.toVO(null)).isNull();
    }

    @Test
    @DisplayName("VO 不暴露 deleted / version")
    void voDoesNotExposeInternalFields() {
        assertThat(MaterialVO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("deleted", "version");
    }
}
