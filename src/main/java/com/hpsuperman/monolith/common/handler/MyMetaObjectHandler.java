package com.hpsuperman.monolith.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId != null) {
            strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
