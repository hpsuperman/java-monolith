package com.hpsuperman.monolith.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity")
class BaseEntityTest {
    @Data
    @TableName("test_entity")
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class Fixture extends BaseEntity {
        private String name;
    }

    @Test
    @DisplayName("id 不同的实体不能相等——漏写 callSuper = true 时会相等，且不报错")
    void equalsIncludesInheritedFields() {
        Fixture a = fixture(1L);
        Fixture b = fixture(2L);

        assertThat(a).isNotEqualTo(b);

        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("id 相同、业务字段也相同的两个实体相等——callSuper 没被写成「永远不等」")
    void equalsIsStillReflexiveAcrossInstances() {
        assertThat(fixture(1L)).isEqualTo(fixture(1L));
    }

    @Test
    @DisplayName("toString 里必须有 id，否则日志里看不到主键")
    void toStringIncludesInheritedFields() {
        assertThat(fixture(42L).toString()).contains("id=42");
    }

    @Test
    @DisplayName("上移到 BaseEntity 的注解仍然跟在字段上——丢了的话映射会静默失效")
    void inheritedFieldsKeepTheirMapperAnnotations() throws Exception {
        assertThat(findField(Fixture.class, "id").getAnnotation(TableId.class)).isNotNull();

        assertThat(fillOf(Fixture.class, "createTime")).isEqualTo(FieldFill.INSERT);
        assertThat(fillOf(Fixture.class, "updateTime")).isEqualTo(FieldFill.INSERT_UPDATE);
    }

    @Test
    @DisplayName("公共字段确实声明在 BaseEntity 上，而不是又被抄回了子类")
    void commonFieldsLiveOnTheBaseClassOnly() throws Exception {
        for (String name : new String[]{"id", "createTime", "updateTime"}) {
            assertThat(BaseEntity.class.getDeclaredField(name))
                    .as("%s 应声明在 BaseEntity 上", name)
                    .isNotNull();
        }

        assertThat(declaredFieldNames(Fixture.class)).doesNotContain(
                "id", "createTime", "updateTime");
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name + " 在 " + type.getName() + " 及其父类中都不存在");
    }

    private static FieldFill fillOf(Class<?> type, String name) throws NoSuchFieldException {
        TableField annotation = findField(type, name).getAnnotation(TableField.class);
        assertThat(annotation).as("%s 上应有 @TableField", name).isNotNull();
        return annotation.fill();
    }

    private static java.util.List<String> declaredFieldNames(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredFields()).map(Field::getName).toList();
    }

    private static Fixture fixture(Long id) {
        Fixture fixture = new Fixture();
        fixture.setId(id);
        fixture.setName("夹具");
        return fixture;
    }
}
