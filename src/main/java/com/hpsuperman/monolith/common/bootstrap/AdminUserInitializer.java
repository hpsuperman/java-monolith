package com.hpsuperman.monolith.common.bootstrap;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.init-admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserInitializer implements ApplicationRunner {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";
    private static final String ADMIN_ROLES = "ADMIN,USER";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userService.getByUsername(ADMIN_USERNAME) != null) {
                return;
            }

            User admin = new User();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
            admin.setNickname("超级管理员");
            admin.setStatus(EnabledStatus.ENABLED);
            admin.setRoles(ADMIN_ROLES);
            userService.save(admin);

            log.warn("""

                            ============================================================
                            已创建默认管理员账号：{} / {}
                            请立即修改密码；生产环境请设置 app.init-admin.enabled=false
                            ============================================================""",
                         ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.error("""

                    ============================================================
                    初始化管理员账号失败。请依次确认：

                      1) 数据库账号密码是否正确
                         用户默认 root，密码取自 ${MYSQL_PASSWORD:root}。
                         若与本机实际不符，用环境变量覆盖，例如：
                           IDEA: Run Configuration -> Environment variables
                           MYSQL_PASSWORD=你的密码;REDIS_PASSWORD=你的密码
                         Redis 同理，密码取自 ${REDIS_PASSWORD:}（默认为空）

                      2) 连的是不是预期的库
                         若连到一个「非空、但没有 sys_user」的库（典型是 url 里库名写错，
                         连到了同实例上另一个库），Flyway 会打上基线、跳过 V1、只建出
                         biz_material，应用照常启动，直到这条查询才报「表不存在」。
                         确认 datasource.url 里的库名，并看启动日志里 Flyway 那几行。

                         （迁移本身失败的话，应用在启动阶段就挂了，不会走到这里。）
                    ============================================================""", e);
            throw e;
        }
    }
}
