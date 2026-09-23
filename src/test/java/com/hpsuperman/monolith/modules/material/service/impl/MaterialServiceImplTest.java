package com.hpsuperman.monolith.modules.material.service.impl;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.modules.material.dto.MaterialUpdateRequest;
import com.hpsuperman.monolith.modules.material.entity.Material;
import com.hpsuperman.monolith.modules.material.mapper.MaterialMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialServiceImpl")
class MaterialServiceImplTest {
    private static final Long MATERIAL_ID = 100L;

    @Mock
    private MaterialMapper materialMapper;

    private MaterialServiceImpl materialService;

    @BeforeEach
    void setUp() {
        materialService = new MaterialServiceImpl();
        ReflectionTestUtils.setField(materialService, "baseMapper", materialMapper);

        assertThat(materialService.getBaseMapper()).isSameAs(materialMapper);
    }

    @Test
    @DisplayName("详情查不到时报「物料不存在」")
    void reportsMissingMaterialOnDetail() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(null);

        assertThatThrownBy(() -> materialService.getDetail(MATERIAL_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("物料不存在");
    }

    @Test
    @DisplayName("详情查得到时转成 VO")
    void returnsVOWhenMaterialExists() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());

        assertThat(materialService.getDetail(MATERIAL_ID).getCode()).isEqualTo("M-001");
    }

    @Test
    @DisplayName("更新时物料不存在就报错，不做任何写入")
    void reportsMissingMaterialOnUpdate() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(null);

        assertThatThrownBy(() -> materialService.update(MATERIAL_ID, new MaterialUpdateRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("物料不存在");

        verify(materialMapper, never()).updateById(any(Material.class));
    }

    @Test
    @DisplayName("乐观锁冲突时抛冲突码，而不是当成成功")
    void reportsConflictWhenOptimisticLockMisses() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());

        when(materialMapper.updateById(any(Material.class))).thenReturn(0);

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setName("新名称");

        assertThatThrownBy(() -> materialService.update(MATERIAL_ID, request))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ResultCode.DATA_CONFLICT.getCode());
    }

    @Test
    @DisplayName("更新必须带上数据库里的当前版本号——漏了拦截器会静默失效，不报任何错")
    void writesCurrentVersionForOptimisticLock() {
        Material existing = material();
        existing.setVersion(7);
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(existing);
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setName("新名称");

        materialService.update(MATERIAL_ID, request);

        assertThat(capturedUpdate().getVersion()).isEqualTo(7);
    }

    @Test
    @DisplayName("备注传空串 = 清空，写库的是空串而不是 null")
    void writesEmptyStringWhenClearingText() {
        stubSuccessfulUpdate();

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setRemark("");

        materialService.update(MATERIAL_ID, request);

        assertThat(capturedUpdate().getRemark()).isEmpty();
    }

    @Test
    @DisplayName("备注传 null = 不修改，实体保持 null 好让 SQL 略过它")
    void leavesTextUntouchedWhenNull() {
        stubSuccessfulUpdate();

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setName("只改名称");

        materialService.update(MATERIAL_ID, request);

        assertThat(capturedUpdate().getRemark()).isNull();
        assertThat(capturedUpdate().getSpec()).isNull();
        assertThat(capturedUpdate().getImageUrl()).isNull();
    }

    @Test
    @DisplayName("全空白文本归一成空串，等同于清空而不是存一串空格")
    void treatsWhitespaceAsClear() {
        stubSuccessfulUpdate();

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setRemark("   ");

        materialService.update(MATERIAL_ID, request);

        assertThat(capturedUpdate().getRemark()).isEmpty();
    }

    @Test
    @DisplayName("数值字段传 0 = 清空（本模块不支持清回未维护）")
    void writesZeroWhenClearingNumber() {
        stubSuccessfulUpdate();

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setPrice(BigDecimal.ZERO);

        materialService.update(MATERIAL_ID, request);

        assertThat(capturedUpdate().getPrice()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("名称传空白串被拒——NOT NULL 列不允许被清空")
    void rejectsClearingRequiredName() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setName("   ");

        assertThatThrownBy(() -> materialService.update(MATERIAL_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessage("物料名称不能清空");

        verify(materialMapper, never()).updateById(any(Material.class));
    }

    @Test
    @DisplayName("计量单位传空白串同样被拒")
    void rejectsClearingRequiredUnit() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setUnit("");

        assertThatThrownBy(() -> materialService.update(MATERIAL_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessage("计量单位不能清空");
    }

    @Test
    @DisplayName("必填字段不传是允许的——null 的语义是「不修改」，不是「清空」")
    void allowsOmittingRequiredFields() {
        stubSuccessfulUpdate();

        materialService.update(MATERIAL_ID, new MaterialUpdateRequest());

        assertThat(capturedUpdate().getName()).isNull();
        assertThat(capturedUpdate().getUnit()).isNull();
    }

    @Test
    @DisplayName("更新返回的是回读后的完整对象，而不是只含本次改动字段的实体")
    void returnsRereadEntityAfterUpdate() {
        Material reread = material();
        reread.setRemark("库里的备注");
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material(), reread);
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setName("只改名称");

        assertThat(materialService.update(MATERIAL_ID, request).getRemark()).isEqualTo("库里的备注");
    }

    @Test
    @DisplayName("删除时物料不存在就报错，不做任何删除")
    void reportsMissingMaterialOnDelete() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(null);

        assertThatThrownBy(() -> materialService.delete(MATERIAL_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("物料不存在");

        verify(materialMapper, never()).deleteById(any(Serializable.class));
    }

    @Test
    @DisplayName("删除成功")
    void deletesMaterial() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());
        when(materialMapper.deleteById(MATERIAL_ID)).thenReturn(1);

        materialService.delete(MATERIAL_ID);

        verify(materialMapper).deleteById(MATERIAL_ID);
    }

    @Test
    @DisplayName("删除返回 0 时报「删除失败」，不能静默当成成功")
    void reportsFailureWhenRemoveReturnsZero() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());
        when(materialMapper.deleteById(MATERIAL_ID)).thenReturn(0);

        assertThatThrownBy(() -> materialService.delete(MATERIAL_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("删除失败");
    }

    private void stubSuccessfulUpdate() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material());
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);
    }

    private Material capturedUpdate() {
        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private static Material material() {
        Material material = new Material();
        material.setId(MATERIAL_ID);
        material.setCode("M-001");
        material.setName("六角螺栓");
        material.setUnit("个");
        material.setStatus(EnabledStatus.ENABLED);
        material.setStock(100);
        material.setVersion(1);
        return material;
    }
}
