package com.hpsuperman.monolith.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.hpsuperman.monolith.modules.material.entity.Material;
import com.hpsuperman.monolith.modules.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity")
class BaseEntityTest {
    @Test
    @DisplayName("id 不同的实体不能相等——漏写 callSuper = true 时会相等，且不报错")
    void equalsIncludesInheritedFields() {
        Material a = material(1L);
        Material b = material(2L);

        assertThat(a).isNotEqualTo(b);

        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("id 相同、业务字段也相同的两个实体相等——callSuper 没被写成「永远不等」")
    void equalsIsStillReflexiveAcrossInstances() {
        assertThat(material(1L)).isEqualTo(material(1L));
    }

    @Test
    @DisplayName("toString 里必须有 id，否则日志里看不到主键")
    void toStringIncludesInheritedFields() {
        assertThat(material(42L).toString()).contains("id=42");
    }

    @Test
    @DisplayName("User 的 toString 依然不含密码——callSuper 不能把 exclude 冲掉")
    void userToStringStillHidesPassword() {
        User user = new User();
        user.setId(7L);
        user.setUsername("admin");
        user.setPassword("$2a$10$THIS_MUST_NOT_LEAK");

        assertThat(user.toString())
                .contains("admin")
                .contains("id=7")
                .doesNotContain("THIS_MUST_NOT_LEAK")
                .doesNotContain("password");
    }

    @Test
    @DisplayName("上移到 BaseEntity 的注解仍然跟在字段上——丢了的话映射会静默失效")
    void inheritedFieldsKeepTheirMapperAnnotations() throws Exception {
        assertThat(findField(Material.class, "id").getAnnotation(TableId.class)).isNotNull();
        assertThat(findField(Material.class, "deleted").getAnnotation(TableLogic.class)).isNotNull();
        assertThat(findField(Material.class, "version").getAnnotation(Version.class)).isNotNull();

        assertThat(fillOf(Material.class, "createTime")).isEqualTo(FieldFill.INSERT);
        assertThat(fillOf(Material.class, "updateTime")).isEqualTo(FieldFill.INSERT_UPDATE);
        assertThat(fillOf(Material.class, "createBy")).isEqualTo(FieldFill.INSERT);
        assertThat(fillOf(Material.class, "updateBy")).isEqualTo(FieldFill.INSERT_UPDATE);
    }

    @Test
    @DisplayName("公共字段确实声明在 BaseEntity 上，而不是又被抄回了子类")
    void commonFieldsLiveOnTheBaseClassOnly() throws Exception {
        for (String name : new String[]{"id", "deleted", "version",
                "createTime", "updateTime", "createBy", "updateBy"}) {
            assertThat(BaseEntity.class.getDeclaredField(name))
                    .as("%s 应声明在 BaseEntity 上", name)
                    .isNotNull();
        }

        assertThat(declaredFieldNames(Material.class)).doesNotContain(
                "id", "deleted", "version", "createTime", "updateTime", "createBy", "updateBy");
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

    private static Material material(Long id) {
        Material material = new Material();
        material.setId(id);
        material.setCode("M-001");
        material.setName("六角螺栓");
        return material;
    }
}
