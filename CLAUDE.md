# CLAUDE.md

Spring Boot 3 单体服务脚手架。**没有 README，这份是唯一文档。仓库公开**
（`github.com/hpsuperman/java-monolith`），提交前必查有没有真实凭据。

**定位**：电商作品集，做小做深，每阶段能单独演示。阶段 0 用户 + 文件上传（已实测）；
阶段 1 商品（已写完未实测）；后续 ②购物车+下单 ③订单状态机 ④幂等/秒杀/缓存。阶段 1 不做 SKU。
**下一步**：前台商品接口 `GET /products` 列表 + 详情，匿名可读，要进白名单。

JDK 17 · Boot 3.5.16 · MyBatis-Plus 3.5.17 · MySQL 8 · Redis 7 · RocketMQ 5 · MinIO 8.5.17 ·
Flyway · springdoc 2.9.1 · jjwt 0.13.0。版本坑写在 pom.xml 注释里。

## 协作方式

**用户是初学者，自己在写代码。给思路和步骤，不要直接落盘成文件。**

- 一次只推进一步（一个文件 / 一个概念），他说「下一步」再继续
- 讲清怎么写简化
- 别一次甩多个文件的完整代码

## 命令

```bash
docker compose up -d     # mysql / redis / rocketmq / minio（含建桶）
mvn spring-boot:run      # 7070
mvn test                 # 32 个纯 Mockito 单测，不连中间件
mvn verify               # 额外跑 spotless
mvn spotless:apply       # 删无用 import、去行尾空白、CRLF→LF
```

本地开发**不用注入密码**（`application-dev.yml` 默认值就是 Docker 容器的值）。

## 结构

```
common/          基础设施，与业务无关。**不依赖 modules**
modules/user/    注册 登录 刷新 登出 改资料（/auth）；用户管理（/admin/users）
modules/product/ 商品（/admin/products）；前台 /products 待写
modules/file/    文件上传（/files）。**没有表，不落库**
resources/       application{,-dev,-prod}.yml · logback · db/migration/V{1,2}__*.sql
test/            4 个类 / 32 用例，纯 Mockito，不起 Spring
```

**依赖方向是硬约束**：`common` 不依赖 `modules`，模块间只走 Service 接口。
**请求链路**：`JwtAuthenticationFilter` → `SecurityFilterChain`（白名单匿名，其余 `authenticated()`）
→ Controller → Service → Mapper；401/403 由 `Rest{EntryPoint,DeniedHandler}` 出。
新模块照 `modules/user/` 抄：`entity`(继承 BaseEntity) + `mapper` + `service`+`impl` + `dto` +
`controller`。建表写 `V{n}__init_xxx.sql`，重启即可。

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

## 规则

- **实体** 继承 `BaseEntity`（只有 id/createTime/updateTime），子类必须显式
  `@EqualsAndHashCode(callSuper=true)` + `@ToString(callSuper=true)`，漏了不报错但父类字段全被排除；
  存密码的 `exclude = "password"`
- **更新语义** `update-strategy: not_null`，传 null = 不改，清空传空字符串。时间列自动填充，
  **其余列建表必须给默认值**，否则插入报 `Field 'xxx' doesn't have a default value`
- **逻辑删除** 按实体单独加。⚠️ `sys_user.deleted` 是**反的**（1=正常 0=已删），必须
  `@TableLogic(value = "1", delval = "0")`；商品表是正常语义，裸 `@TableLogic`
- **分页** 条件继承 `common/dto/PageQuery`，`page(query.toPage(), wrapper)` →
  `PageResult.of(page, XxxConverter::toVO)`。`BlockAttackInnerInterceptor` 挡全表更新/删除，别摘
- **权限** 白名单 `SecurityConfig.PUBLIC_PATHS` 是**精确路径，不是通配**：新 auth 接口必须进，
  要登录的不能进；除 auth 外必须写显式 `@PreAuthorize`（兜底只是「登录即可」）；
  管理端和前台共用同一资源时必须分路径（`/admin/products` vs `/products`）
- **认证** JWT HS256，secret ≥ 32 字节（不足启动失败）。access 30m / refresh 7d，带 `jti` 轮换。
  claim 只有 id / phone / roles，**没有 nickname**。`TokenStore` 读 Redis 异常**按已撤销处理
  （fail-closed）**，别改。⚠️ `LoginUser.isEnabled()` **恒为 true**，判账号状态只能查库
- **测试** 纯 Mockito。⚠️ 没打桩的 `int` 返回 0，写路径用例必须显式 `thenReturn(1)`
- **风格** 源码注释已剥光，**新代码不写注释**。4 空格，LF。spotless 没开完整格式化器，别顺手开

## 模块

**user** `POST /auth/{register,login,refresh,logout}` + `PUT|GET /auth/{updateProfile,getProfile}`，
后两个要登录、不在白名单。

- 注册直接返 token，`roles` 服务端写死 `USER`。请求对象和实体**绝不能复用**
- **登录失败抛 `BizException`**（HTTP 200），别抛 `UNAUTHORIZED`——会变成「200 + code 401」，
  前端以为 token 过期去跳登录页，而用户本来就在登录页
- ⚠️ **profile 两个接口 + refresh 判「账号没了/被封」必须抛 `BadCredentialsException`（→401）**，
  抛 `BizException` 会变成「200 + code 500」，前端永远等不到跳转
- 登录**先验密码再判 status**，反过来随便试个手机号就能问出「这号存在但被禁用」
- ⚠️ profile 两个接口都先过 `requireEnabledUser(userId)`（查库判 status），只看 token 的话
  封号后那条 access 还能用满 30 分钟
- `updateProfile` 三字段全 null 直接拒，否则空 body `{}` 也回成功
- ⚠️ **JWT 不带 nickname**（只带 id/phone/roles），昵称唯一来源是库。`JwtTokenProviderTest`
  有一条断言盯着，想加回去先想清楚怎么让它跟着 `updateProfile` 刷新
- ⚠️ **`UserVO` 标了 `@JsonInclude(ALWAYS)`**，是全项目 `default-property-inclusion: non_null`
  的例外；不加的话头像/邮箱为 null 时整个 key 消失，前端分不清「没设」和「没这个字段」

**自操作守卫**：重建 user 时挡「禁用自己」「摘掉自己的 ADMIN」「删自己」。挡不住 A 禁 B、B 禁 A。

**product** `/admin/products` 增删改查，ADMIN 专用。表 `sys_product`，软删。没有自操作守卫。

**file** `POST /files`，`isAuthenticated()`，不在白名单。中转上传、bucket 公开读。校验顺序：
扩展名白名单 → 大小 → 文件头魔数。`filename` / `Content-Type` 一律不采信，扩展名绝不进存储路径。

- 加 pdf/docx 要同步补 `SIGNATURES` 魔数表
- ⚠️ 大小上限只有一个来源 `spring.servlet.multipart.max-file-size` = `${app.minio.max-file-size}`
- ⚠️ `endpoint`（应用连 MinIO）≠ `public-url`（拼给浏览器）
- ⚠️ multipart 三类异常各接各的，少接一个就掉 `Exception` 兜底变 500：
  `MaxUploadSizeExceeded`(413) / `MissingServletRequestPart`(400) / `MultipartException`(400)
- `PutObjectArgs` 没设 `contentType`，存出来都是 `application/octet-stream`
- 桶由 compose 的 `minio-init` 建，**别在启动时建桶**

## Flyway

- **已执行过的迁移一个字都不能改**（校验和 mismatch → 启动失败），**改注释也会变**（已踩过）
- **只增不减**，不写 DROP。撞版本号就改名
- ⚠️ **别手工删 `flyway_schema_history` 的行**：表还在账本没了 → 重跑 V1 → 撞「表已存在」→
  记一条 `success=0`，之后每次启动都卡在这
- ⚠️ **删了迁移文件必须 `mvn clean`**，否则 `target/classes` 里的旧拷贝会重跑
- 重来就 `DROP DATABASE` 重建，别手工 DROP 单表

## 已知限制（刻意的取舍，别顺手修）

1. Redis 挂掉时带令牌的请求一律 503；启动强依赖数据库可达
2. 角色是逗号分隔串，不是 RBAC 三张表；两个管理员可以互相锁在门外
3. 上传的文件不自动清理，没做病毒扫描和图片二次编码
4. **登出只能全端登出**（`revokeUserTokens` 按 userId 整体撤销）；`blacklist(jti)` 没有调用方
5. `auth:refresh:{userId}` 只有一个 key → 隐式单端登录，设备 B 一登录 A 的 refresh 就刷不动
6. `uk_phone` 不含 `deleted`，软删掉的手机号永久占位，不能重新注册

## 上线前

`JWT_SECRET` 换强密钥 · CORS 改具体域名 · 关 springdoc · 删 `application-dev.yml` 的 `log-impl` ·
MySQL 密码走环境变量 · 库账号非 root 但**必须给 CREATE/ALTER/INDEX**（Flyway 要 DDL）·
确认 `clean-disabled: true` · 部署顺序「先数据库、后应用」· MinIO 换掉 minioadmin、
桶策略只给 `GetObject`（**别给 ListBucket**）· 配 `MINIO_PUBLIC_URL` 并确认外网可达 ·
上传接口挂限流
