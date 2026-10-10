# CLAUDE.md

Spring Boot 3 单体服务脚手架。**仓库里没有 README，这份是唯一文档。仓库公开**
（`github.com/hpsuperman/java-monolith`），提交前必查有没有真实凭据。

**状态**：`common/` 完整可用；`modules/user/` 六个接口**全部实测过**（注册/登录/refresh/logout/
改资料/查资料）；`modules/file/` 上传已实测。
**业务层零测试**——4 个类 32 个用例全在 `common/`，`modules/` 里一行测试都没有。

JDK 17 · Boot 3.5.16 · MyBatis-Plus 3.5.17 · MySQL 8 · Redis 7 · RocketMQ 5 · MinIO 8.5.17 ·
Flyway · springdoc 2.9.1 · jjwt 0.13.0。版本坑写在 pom.xml 注释里。

## 协作方式

**用户是初学者，自己在写代码。给思路和步骤，不要直接落盘成文件。**

- 一次只推进一步（一个文件 / 一个概念），他说「下一步」再继续
- 讲清「写什么 / 为什么 / 不写会怎样」——**必须说后果**，只说「必须写」他记不住
- 别一次甩多个文件的完整代码

## 命令

```bash
docker compose up -d     # mysql / redis / rocketmq / minio（含建桶）
mvn spring-boot:run      # 7070
mvn test                 # 32 个纯 Mockito 单测，不连中间件
mvn verify               # 额外跑 spotless
mvn spotless:apply       # 无损：删无用 import、去行尾空白、补末尾换行、CRLF→LF
```

本地开发**不用注入密码**（`application-dev.yml` 的默认值就是 Docker 容器的值）。
`/actuator/health` 通就说明基础设施接上了。

## 结构

```
common/         基础设施，与业务无关。**不依赖 modules**
                config 9 · security 9 · result 3 · exception 2 · entity/dto/handler/enums 各 1-2
modules/user/   注册 登录 刷新 登出 改资料（路由 /api/auth）
modules/file/   文件上传（/api/files）。**没有表，不落库**
resources/      application{,-dev,-prod}.yml · logback · db/migration/V1__init_sys_user.sql
test/           4 个类 / 32 用例，纯 Mockito，不起 Spring
```

**依赖方向是硬约束**：`common` 不依赖 `modules`，模块间只走 Service 接口。按领域分包是为了
将来能整包搬走。曾被 `common/bootstrap/AdminUserInitializer` 违反，文件已删，现在没有例外。

**请求链路**：`JwtAuthenticationFilter`（Bearer → 查黑名单 → SecurityContext）→
`SecurityFilterChain`（白名单匿名，其余 `authenticated()`）→ Controller → Service → Mapper。
401/403 由 `Rest{EntryPoint,DeniedHandler}` 出。

新模块照 `modules/user/` 抄：`entity`(继承 BaseEntity) + `mapper` + `service`+`impl` +
`dto` + `controller`。`@MapperScan` 和 `type-aliases-package` 已配好，不用逐个 `@Mapper`。
建表写 `V2__init_xxx.sql`，重启即可。

## 响应码

| 场景 | HTTP | code |
|---|---|---|
| 成功 | 200 | 200 |
| 业务失败 | **200** | 500 |
| 参数校验 / 缺参数 / 缺 multipart 部件 / 非 multipart | 400 | 400 |
| 未认证 / 过期 | 401 | 401 |
| 无权限 | 403 | 403 |
| 资源不存在 / 方法不支持 | 404 / 405 | 404 / 405 |
| 唯一键冲突 | 409 | 500 |
| 上传超容器上限 | 413 | 413 |
| 系统异常 | 500 | 500 |

业务失败返 200 是刻意的（前端统一判 `success`）；认证类必须 HTTP 准确，否则跳不了登录页。
**`code` 区分不了原因**——BIZ_ERROR / DATA_CONFLICT / 系统异常都是 500，原因只在 `message`。

## 规则

**实体** 继承 `BaseEntity`（只有 id/createTime/updateTime），子类必须显式
`@EqualsAndHashCode(callSuper=true)` + `@ToString(callSuper=true)`——Lombok 默认 false，
漏了不报错但父类字段全被排除。存密码的 `exclude = "password"`。

**更新语义** `update-strategy: not_null`，传 null = 不改，清空传空字符串。时间列自动填充；
**其余列必须在建表里给默认值**，否则插入报 `Field 'xxx' doesn't have a default value`。

**逻辑删除** `BaseEntity` 里已删掉，按实体单独加。⚠️ `sys_user.deleted` 是**反的**（1=正常 0=已删），
必须 `@TableLogic(value = "1", delval = "0")`——只写 `@TableLogic` 会让删除逻辑整个反过来。

**分页** 查询条件继承 `common/dto/PageQuery`，`page(query.toPage(), wrapper)` →
`PageResult.of(page, XxxConverter::toVO)`。`BlockAttackInnerInterceptor` 挡全表更新/删除，别摘。

**权限** 白名单是 `SecurityConfig.PUBLIC_PATHS`（**精确路径，不是通配**）。
⚠️ 新 auth 接口**必须**进白名单——`register` 漏加过：没 token 的人被 `authenticated()` 拦住，
死锁（没账号 → 登不了 → 拿不到 token）。
⚠️ 但 `getProfile` / `updateProfile` 这类要登录的**不能**进，别图省事把 `/api/auth` 写成通配。
⚠️ 除 auth 外必须写显式 `@PreAuthorize`——兜底只是「登录即可」，`/api/users` 查询曾靠兜底，
任何账号都能翻出全库手机号邮箱。

**认证** JWT HS256，secret ≥ 32 字节（不足启动失败）。access 30m / refresh 7d，refresh 带 `jti` 轮换。
claim 只有 id / phone / roles——**没有 nickname**（改过，见 modules/user）。
`TokenStore` 读 Redis 异常**按已撤销处理（fail-closed）**，别改成 fail-open。
⚠️ `TokenStore.blacklist` **至今没有调用方**。登出走的是 `revokeUserTokens`（按 userId 整体撤销，
`issuedAt <= 撤销时刻` 的令牌全失效；标记活 35 分钟 > access 的 30 分钟，所以必然覆盖）。
**登出是真有效的**，代价是全端登出——没法只踢一台设备（见限制 7）。
⚠️ `LoginUser.fromClaims` 把 status 硬编码成 `ENABLED`，所以 **`loginUser.isEnabled()` 恒为 true**：
它只说明「token 签名有效」，不说明账号现在还能用。判账号状态只能查库。

**测试** 纯 Mockito。⚠️ 没打桩的 `int` 返回 0，写路径用例必须显式 `thenReturn(1)`，
否则会误判成「操作没生效」而去翻业务代码。

**风格** 源码注释已剥光，**新代码不写注释**。4 空格，LF（spotless `lineEndings: UNIX`）。
spotless 刻意没开完整格式化器，别顺手开。

## modules/user

`POST /api/auth/{register,login,refresh,logout}` + `PUT|GET /api/auth/{updateProfile,getProfile}`。
后两个要登录，**不在白名单**。

- 注册直接返 token；`roles` 服务端写死 `USER`，`RegisterRequest` 里**没有 roles 字段**。
  请求对象和实体长得像，但**绝不能复用实体**（roles/status/deleted 都是服务端管的）
- **登录失败抛 `BizException`**（永远 HTTP 200）。别抛 `UNAUTHORIZED`——那会变成「200 + code 401」，
  前端认定 token 过期去跳登录页，而用户本来就在登录页
- 登录**先验密码再判 status**，反过来随便试个手机号就能问出「这号存在但被禁用」
- ⚠️ **profile 两个接口 + refresh 里「账号没了/被封」必须抛 `BadCredentialsException`（→401）**，
  不是 `BizException`。照抄 login 的写法会变成「200 + code 500」，前端永远等不到跳转
- ⚠️ **profile 两个接口都先过 `requireEnabledUser(userId)`（查库判 status）**。只看 token 的话，
  封号后那条 access 还能用满 30 分钟。`isEnabled()` 帮不上忙，它恒 true
- `updateProfile` 传 null = 不改（`update-strategy: not_null`）；但**三个字段全 null 直接拒**，
  否则空 body `{}` 也回成功，前端以为改上去了
- `avatar` 是 `TEXT`（65KB），`@Size(max = 500)` 是人定的业务上限不是列宽
- `logout` 故意不抛异常（登出是幂等的），废令牌直接 return
- 唯一键冲突两条路径：预检查 `exists()` → 200/500；并发撞 `uk_phone` → 409。同错两码，已知瑕疵
- ⚠️ **JWT 不再带 nickname**（只带 id/phone/roles）。带过一版，改昵称后 token 里那份就是旧的；
  现在昵称唯一来源是库。`JwtTokenProviderTest` 有一条 `assertThat(...get("nickname")).isNull()` 盯着，
  想加回去先想清楚怎么让它跟着 `updateProfile` 刷新
- ⚠️ **`UserVO` 标了 `@JsonInclude(ALWAYS)`**，是全项目 `default-property-inclusion: non_null` 的
  例外。不加的话头像/邮箱为 null 时整个 key 消失，前端分不清「没设」和「没这个字段」

## 文件上传

`POST /api/files`，`@PreAuthorize("isAuthenticated()")`，**不在白名单**。
中转上传（非预签名）、bucket 公开读。校验顺序：扩展名白名单 → 大小 → 文件头魔数。
`filename` / `Content-Type` 一律不采信，扩展名转小写且**绝不进存储路径**
（对象名 `yyyy/MM/dd/{uuid}.{ext}`）。

- ⚠️ 加 pdf/docx 要同步补 `SIGNATURES` 魔数表，否则新格式必报「文件内容与扩展名不符」
- ⚠️ 大小上限只有一个来源：`spring.servlet.multipart.max-file-size` = `${app.minio.max-file-size}`。
  改成不同字面量则容器那道总是先拦，业务校验永远不执行
- ⚠️ `endpoint`（应用连 MinIO）≠ `public-url`（拼给浏览器）。应用在容器里时 endpoint 是
  `http://minio:9000`，浏览器解析不了
- ⚠️ multipart 三类异常**各接各的**，少接一个就掉进 `Exception` 兜底变 500：
  `MaxUploadSizeExceeded`(413) / `MissingServletRequestPart`(400) / `MultipartException`(400)。
  前者是后者的子类，Spring 按具体类型选，413 不会被 400 抢走
- ⚠️ `PutObjectArgs` 没设 `contentType`，存出来一律 `application/octet-stream`（`<img>` 靠浏览器
  嗅探照样显示，直接开链接会变下载）。要修就从**已校验的扩展名**映射 MIME，别透传请求头的
- 桶由 compose 的 `minio-init` 建，`MinioClient` 构造不发请求。**别在启动时建桶**

**自操作守卫**：重建 user 时挡「禁用自己」「摘掉自己的 ADMIN」「删自己」。挡不住 A 禁 B、B 禁 A。

## Flyway

- **已执行过的迁移一个字都不能改**（校验和 mismatch → 启动失败）。⚠️ **改注释也会变**，已踩过
- **只增不减**，不写 DROP。撞版本号就改名（校验和不算文件名）
- ⚠️ `baseline-version: 1` + `baseline-on-migrate: true`：V1 已成功执行，**内容就此冻结**，
  动它必炸，只能重建库或改走 V2
- ⚠️ **别手工删 `flyway_schema_history` 的行**（已踩过）：表还在、账本没了 → 重跑 V1 →
  撞「表已存在」→ 记一条 `success=0`，之后每次启动都卡在这
- ⚠️ **删了迁移文件必须 `mvn clean`**，否则 `target/classes` 里的旧拷贝会重跑
- 重来就 `DROP DATABASE` 重建，别手工 DROP 单表（基线记下后救不回来）

## 已知限制（刻意的取舍，别顺手修）

1. Redis 挂掉时带令牌的请求一律 503（fail-closed）
2. 启动强依赖数据库可达（Flyway 在启动阶段跑）
3. 角色是逗号分隔串，不是 RBAC 三张表
4. 两个管理员可以互相锁在门外
5. 上传的文件不自动清理，没有引用计数也没有清理任务
6. 上传没做病毒扫描和图片二次编码，魔数只证明「这是张 PNG」
7. **登出只能全端登出**：`revokeUserTokens` 按 userId 整体撤销，登出后该用户**所有**设备的
   access/refresh 一起失效（实测确认，不是「登出是假的」）。想只踢一台设备得用
   `blacklist(jti)`，而它**至今没有调用方**
8. `auth:refresh:{userId}` 只有一个 key → **隐式单端登录**：设备 B 一登录，设备 A 的 refresh
   就刷不动了（A 那条 access 还能用到过期）
9. `uk_phone` 不含 `deleted`，软删掉的手机号**永久占位**，不能重新注册

## 上线前

`JWT_SECRET` 换强密钥 · CORS 改具体域名 · 关 springdoc · 删 `application-dev.yml` 的 `log-impl` ·
MySQL 密码走环境变量 · 库账号非 root 但**必须给 CREATE/ALTER/INDEX**（Flyway 要 DDL）·
确认 `clean-disabled: true` · 部署顺序「先数据库、后应用」· MinIO 换掉 minioadmin、
桶策略只给 `GetObject`（**别给 ListBucket**，否则枚举桶就拿到全部文件名）· 配 `MINIO_PUBLIC_URL`
并确认外网可达 · 上传接口挂限流。
