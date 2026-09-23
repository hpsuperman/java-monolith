# CLAUDE.md

Spring Boot 3 单体脚手架。**`common/` 完整，`modules/` 空待重建，无迁移文件。**
28 主文件 / 1389 行 + 4 测试类 / 529 行（32 用例）。

JDK 17 · Spring Boot 3.5.16 · MyBatis-Plus 3.5.17 · MySQL 8 · Redis 7 · RocketMQ 5 ·
MinIO 8.5.17 · Flyway · springdoc 2.9.1 · jjwt 0.13.0。版本坑在 pom.xml 注释里。

**公开仓库** `github.com/hpsuperman/java-monolith`——提交前查凭据。

## 命令

```bash
docker compose up -d     # mysql / redis / rocketmq / minio（含建桶）
mvn spring-boot:run      # 7070
mvn test                 # 32 个纯 Mockito 单测，不连中间件
mvn verify               # 额外跑 spotless
mvn spotless:apply       # 只开无损规则：删无用 import、去行尾空白、补末尾换行
```

本地不用注入密码，`application-dev.yml` 默认值就是 Docker 的（MySQL root/root，Redis 无密码）。

## 目录

```
common/           基础设施，不依赖 modules
├── config/       Security MybatisPlus Redis Cors Jackson OpenApi WebMvc Minio{Properties,Config}
├── security/     JwtTokenProvider JwtAuthenticationFilter TokenStore LoginUser
│                 SecurityUtils BearerTokenResolver JwtProperties Rest{EntryPoint,DeniedHandler}
├── dto/PageQuery 分页入参基类
├── entity/BaseEntity
├── result/       Result PageResult ResultCode
├── exception/    BizException GlobalExceptionHandler
├── handler/      MyMetaObjectHandler
└── enums/        EnabledStatus
modules/          空
resources/db/migration/   空
resources/mapper/         .gitkeep
```

按领域分包，为了拆服务时整包搬走。`common` 不依赖 `modules`，模块间只走 Service 接口。
（曾由 `AdminUserInitializer` 违反，已删——别再把启动期业务逻辑塞回 `common`。）

新模块照抄：`entity extends BaseEntity` → `mapper extends BaseMapper<T>` →
`service` + `impl extends ServiceImpl<XxxMapper, Xxx>` → `dto`(Request/VO/Converter) → `controller`。
`@MapperScan("...modules.**.mapper")` 已配，不用逐个 `@Mapper`。
`SecurityConfig` 已备好 `AuthenticationManager` / `PasswordEncoder`(BCrypt) / `CorsConfigurationSource`。

## 响应码

| 场景 | HTTP | body.code |
|---|---|---|
| 成功 | 200 | 200 |
| 业务失败 / 乐观锁冲突 | **200** | 500 |
| 唯一键冲突 | 409 | 500 |
| 参数校验失败 | 400 | 400 |
| 未认证 / 过期 | 401 | 401 |
| 无权限 | 403 | 403 |
| 上传超容器上限 | 413 | 413 |
| 系统异常 | 500 | 500 |

业务失败返 200 是刻意的，前端统一判 `success`；认证/权限类必须 HTTP 准确，否则跳不了登录页。
**`body.code` 区分不了失败原因**——BIZ_ERROR / DATA_CONFLICT / 系统异常都是 500，原因只在 `message`。
抛 `BizException`；要 409 用 `BizException.throwIf(cond, ResultCode.DATA_CONFLICT, msg)`。

## 规则

**实体**：必须继承 `BaseEntity`，子类必须显式写 `@EqualsAndHashCode(callSuper = true)` 和
`@ToString(callSuper = true)`——Lombok 默认 false，漏了**不报错**但父类字段全被排除（id 不同也 equals）。
存密码的要 `exclude = "password"`。`BaseEntityTest` 盯着。

**更新语义**：`update-strategy: not_null`，传 null = 不修改，不是置空；清空传空字符串。
`deleted`/`version` **不在自动填充里**，靠建表 `NOT NULL DEFAULT 0` 兜底——建表别省。

**乐观锁**：`@Version`，更新前 `update.setVersion(existing.getVersion())`，返回 false 自己转 409。

**逻辑删除**：`deleted`，`lambdaQuery` 自动追加 `deleted = 0`。

**分页**：查询条件继承 `common/dto/PageQuery`，别重复声明 pageNum/pageSize。
Service 里 `page(query.toPage(), wrapper)`，返回 `PageResult.of(page, XxxConverter::toVO)`。
`PageQuery.MAX_PAGE_SIZE`(500) 与 `MybatisPlusConfig` 的 `setMaxLimit` 同源，改一处两处都变。

**权限**：兜底是 `anyRequest().authenticated()`，匿名白名单是 `SecurityConfig.PUBLIC_PATHS`
（auth/login|refresh|logout、actuator/health|info、/error）和 `DOC_PATHS`。
前三个 auth 路径**现在还不存在**，刻意留着让 auth 重建时开箱匿名。
⚠️ 加接口必须写显式 `@PreAuthorize`，别靠兜底——兜底只是「登录即可」。
`/api/users` 查询接口曾经就靠兜底，任何账号都能翻出全库邮箱手机号。

**认证**：JWT HS256，`app.jwt.secret` ≥ 32 字节（构造时校验，不足启动失败）。
access 30m / refresh 7d。refresh 带 `jti` 轮换（Redis Lua CAS，旧的失效）；
登出把 access 的 `jti` 加黑名单；改状态/角色按 userId 整体撤销。
`TokenStore` 读 Redis 异常**按已撤销处理（fail-closed）**，别改成 fail-open。

**测试**：纯 Mockito，不启 Spring。⚠️ Mockito 对未打桩的 `int` 返回 0，写路径用例里会被当成
「乐观锁未命中」误判成 bug——必须显式 `thenReturn(1)`。

**风格**：源码注释已剥光，**新代码不写注释**。4 空格，LF（`.editorconfig` + spotless
`lineEndings: UNIX`，不指定 Windows 会全改 CRLF）。spotless 刻意没开完整格式化器，别顺手开。

## Flyway

- **已执行过的迁移一个字都不能改**（校验和 mismatch → 启动失败）。⚠️ **校验和算内容，改注释也会变**，已踩过
- **只增不减**，不写 DROP TABLE / DROP COLUMN
- 撞版本号就改名（校验和不算文件名）
- ⚠️ 现在**零迁移**，所以 `baseline-version: 1` 会咬人：库存量非空又被打过基线后，**V1 整份跳过且不报错**。
  要么库保持干净，要么新迁移从 **V2** 起编号
- ⚠️ **删了迁移文件必须 `mvn clean`**——Flyway 读 classpath，`target/classes/db/migration/` 的旧拷贝会重跑
- 重来就 `DROP DATABASE` 重建，别手工 DROP 单表（基线记下后救不回来）

## 重建模块时的约束

**文件上传**：`MinioConfig`/`MinioProperties` 和 `app.minio.*` **都还在**，删的只是 `FileService`。
两个已定取舍：中转上传（非直传预签名）、bucket 公开读（URL 永久可存库）。
服务端三重校验：扩展名白名单 → 文件头魔数 → 大小上限。`filename`/`Content-Type` 一律不采信，
前者只取扩展名且**绝不进存储路径**。

- ⚠️ 白名单里有已知签名的扩展名才走魔数校验。加 pdf/docx 要同步补魔数签名表
- ⚠️ 大小上限只有一个来源：`spring.servlet.multipart.max-file-size` = `${app.minio.max-file-size}`，
  别改成不同字面量——容器那道总是先拦，业务校验就永远不执行
- ⚠️ `app.minio.endpoint`（应用连 MinIO）≠ `public-url`（拼进 URL 给浏览器）。
  应用在容器里时 endpoint 写 `http://minio:9000`，浏览器解析不了，public-url 必须填浏览器可达地址
- 应用不依赖 MinIO 可用：桶由 compose 的 `minio-init` 建，`MinioClient` 构造不发请求。**别在启动时建桶**

**自操作守卫**：重建 user 时要挡「禁用自己」「摘掉自己的 ADMIN」「删自己」。挡不住 A 禁 B、B 禁 A。

## 已知限制（刻意的取舍，别顺手修）

1. Redis 挂掉时携带令牌的请求一律 503，登录/刷新/登出全失败（fail-closed）
2. 启动强依赖数据库可达（Flyway 在启动阶段跑）
3. 角色是逗号分隔串，不是 RBAC 三张表
4. 两个管理员可以互相锁在门外
5. 上传的文件不自动清理，没有引用计数也没有清理任务
6. 上传没做病毒扫描和图片二次编码，魔数只证明「这是张 PNG」

## 上线前

`JWT_SECRET` 换强密钥 · CORS 改具体域名 · 关 springdoc ·
删 `application-dev.yml` 的 `log-impl`（打印 SQL 拖性能且泄露数据）· MySQL 密码走环境变量 ·
库账号非 root 但**必须给 CREATE/ALTER/INDEX**（Flyway 要 DDL）· 确认 `clean-disabled: true` ·
部署顺序「先数据库、后应用」· MinIO 换掉 minioadmin、桶策略只给 `GetObject`
（**别给 ListBucket**，否则枚举桶就拿到全部文件名）· 配 `MINIO_PUBLIC_URL` 并确认外网可达 ·
上传接口挂限流。
