# CLAUDE.md

Spring Boot 3 单体服务脚手架。**仓库里没有 README，这份是唯一的项目文档。**

**当前状态：基础设施齐全，业务层刚起步。** `common/` 完整可用；`modules/user/` 有实体、迁移
（`V1__init_sys_user.sql`）和注册/登录的完整链路。**这套业务代码还没在真实环境跑通过一次**。
`/api/auth/refresh` 和 `/api/auth/logout` 没写，`PUBLIC_PATHS` 里是预留的。
4 个测试类 / 529 行（32 个用例）——**业务层目前零测试**。

技术栈：JDK 17 · Spring Boot 3.5.16 · MyBatis-Plus 3.5.17 · MySQL 8 · Redis 7 · RocketMQ 5 ·
MinIO 8.5.17 · Flyway · springdoc 2.9.1 · jjwt 0.13.0。版本坑写在 pom.xml 注释里，升级前先读。

**仓库是公开的**：`github.com/hpsuperman/java-monolith`。提交前必查有没有真实凭据。

## 协作方式

**用户是初学者，自己在写代码。给思路和步骤，不要直接落盘成文件。**

- 一次只推进一步（一个文件 / 一个概念），用户说「下一步」再继续
- 讲清「写什么 / 为什么这么写 / 不写会怎样」。坑必须说明**不写的后果**，只说「必须写」他记不住
- 每个文件他自己写完、验证通过，再进下一步
- 别在一次回答里甩多个文件的完整代码

## 命令

```bash
docker compose up -d     # mysql / redis / rocketmq / minio（含建桶）
mvn spring-boot:run      # 7070
mvn test                 # 32 个纯 Mockito 单测，不连中间件，任何机器都能跑
mvn verify               # 额外跑 spotless
mvn spotless:apply       # 只开无损规则：删无用 import、去行尾空白、补末尾换行
```

Swagger http://localhost:7070/swagger-ui.html · `/actuator/health` 通就说明基础设施接上了。
本地开发**不用注入密码**：`application-dev.yml` 的默认值就是 Docker 容器的值（MySQL root/root，Redis 无密码）。
不要中间件时可以参考 compose 文件注释里的「只起一部分」那行。

## 项目结构

```
src/main/java/com/hpsuperman/monolith/
├── MonolithApplication.java   @SpringBootApplication + @ConfigurationPropertiesScan
│                              + @EnableTransactionManagement
├── common/                    基础设施，与业务无关。**不依赖 modules**
│   ├── config/      9 个       Security MybatisPlus Redis Cors Jackson OpenApi WebMvc
│   │                           Minio{Properties,Config}  ← Cors 是 CorsProperties
│   ├── security/    9 个       JwtTokenProvider JwtAuthenticationFilter TokenStore LoginUser
│   │                           SecurityUtils BearerTokenResolver JwtProperties
│   │                           Rest{EntryPoint,DeniedHandler}
│   ├── result/      3 个       Result PageResult ResultCode
│   ├── exception/   2 个       BizException GlobalExceptionHandler
│   ├── entity/      1 个       BaseEntity
│   ├── dto/         1 个       PageQuery
│   ├── handler/     1 个       MyMetaObjectHandler（自动填充）
│   └── enums/       2 个       EnabledStatus Role
└── modules/user/     业务模块：注册/登录。详见下面「modules/user」一节
    ├── entity/       User（表 sys_user）
    ├── mapper/       UserMapper
    ├── service/      UserService + impl/UserServiceImpl
    ├── dto/          RegisterRequest LoginRequest TokenVO
    └── controller/   UserController（路由前缀是 /api/auth）

src/main/resources/
├── application.yml       主配置：端口 7070、Flyway、MyBatis-Plus、springdoc、app.{jwt,cors,minio}
├── application-dev.yml   开发覆盖：本地中间件地址、打印 SQL、debug 日志
├── application-prod.yml  生产骨架：占位符一律不给默认值（漏配就启动失败，好过静默连本机）
├── logback-spring.xml
└── mapper/.gitkeep       MyBatis XML 放这里（与 mapper 接口同名）

src/test/java/.../common/{config,entity,security}/   4 个测试类，纯 Mockito，不起 Spring
src/main/resources/db/migration/                     V1__init_sys_user.sql
docker-compose.yml + docker/rocketmq/broker.conf     本地基础设施
```

**依赖方向是硬约束**：`common` 不依赖 `modules`，模块之间只走 Service 接口。
按领域分包（不是按技术层平铺），是为了将来拆服务时整个包能搬走。

> 这条约束曾被 `common/bootstrap/AdminUserInitializer` 违反（为建默认管理员而反向依赖
> `modules.user`）。该文件已随模块一起删掉，约束现在没有例外。重建时别再把启动期业务逻辑
> 塞回 `common`——那里只放与业务无关的基础设施。

**一次请求的链路**：`JwtAuthenticationFilter`（解析 Bearer → 查 Redis 黑名单 → 塞
`SecurityContext`）→ `SecurityFilterChain`（`PUBLIC_PATHS`/`DOC_PATHS` 匿名，其余
`authenticated()`）→ Controller（`@PreAuthorize` 把权限收紧）→ Service → Mapper。
认证/鉴权失败由 `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` 返回 401/403。

## 加一个新模块

```
modules/xxx/
├── entity/      Xxx extends BaseEntity
├── mapper/      XxxMapper extends BaseMapper<Xxx>
├── service/     XxxService 接口 + impl/XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx>
├── dto/         XxxRequest / XxxVO / XxxConverter
└── controller/  XxxController
```

建表写 `V2__init_xxx.sql`（**编号规则见下面 Flyway 那节的陷阱**），重启应用即可，不用手工跑 SQL。

`@MapperScan("com.hpsuperman.monolith.modules.**.mapper")` 已配在 `MybatisPlusConfig` 上，
不用逐个 `@Mapper`；`type-aliases-package` 也已指向 `modules.**.entity`。
`SecurityConfig` 已经备好 `AuthenticationManager`、`PasswordEncoder`(BCrypt)、
`CorsConfigurationSource`，直接注入，别重复配。

## modules/user（注册 / 登录）

`POST /api/auth/register` 和 `POST /api/auth/login`，都返回 `TokenVO`（access + refresh 两条）。

- **注册直接返回 token**，前端不用再调一次登录
- `roles` 由服务端写死 `Role.USER.name()`。`RegisterRequest` 里**没有 roles 字段**，
  前端传了也接不到——这是防「自己把自己封成管理员」的第一道闸。
  请求对象和实体长得像，但**绝不能复用实体**：`roles`/`status`/`deleted` 都是服务端管的
- **登录失败抛 `BizException`，不要抛 `UNAUTHORIZED`**：`handleBizException` 是
  `ResponseEntity.ok(...)`，永远 HTTP 200；抛 401 会变成「HTTP 200 + code 401」，
  而前端把 401 认作 token 过期会去跳登录页——用户本来就在登录页
- 登录先验密码、**再**判 `status`。反过来写，随便试个手机号就能问出「这号存在但被禁用了」
- 唯一键冲突有两条路径：Service 预检查 `exists()` → 200/500；并发撞 `uk_phone` →
  `DuplicateKeyException` → GlobalExceptionHandler → **409**。同一种错两种码，已知瑕疵
- `deleted` **不用手动设**，建表有 `DEFAULT 1`，MP 的 insert 遇到 null 会跳过该列

## 响应码

| 场景 | HTTP | body.code |
|---|---|---|
| 成功 | 200 | 200 |
| 业务失败 | **200** | 500 |
| 唯一键冲突 | 409 | 500 |
| 参数校验失败 | 400 | 400 |
| 未认证 / 过期 | 401 | 401 |
| 无权限 | 403 | 403 |
| 资源不存在 | 404 | 404 |
| 方法不支持 | 405 | 405 |
| 上传超容器上限 | 413 | 413 |
| 系统异常 | 500 | 500 |

业务失败返 200 是刻意的，前端统一判 `success`；认证/权限类必须 HTTP 准确，否则跳不了登录页。
**`body.code` 区分不了失败原因**——BIZ_ERROR / DATA_CONFLICT / 系统异常都是 500，原因只在 `message`。
抛 `BizException`；要 409 用 `BizException.throwIf(cond, ResultCode.DATA_CONFLICT, msg)`。

## 规则

**实体**：必须继承 `BaseEntity`（只给 `id`/`createTime`/`updateTime` 三个字段），子类必须显式写
`@EqualsAndHashCode(callSuper = true)` 和 `@ToString(callSuper = true)`——Lombok 默认 false，
漏了**不报错**但父类字段全被排除（id 不同也 equals）。存密码的要 `exclude = "password"`。`BaseEntityTest` 盯着。

**更新语义**：`update-strategy: not_null`，传 null = 不修改，不是置空；清空传空字符串。
`createTime`/`updateTime` 由 `MyMetaObjectHandler` 自动填充；**其余列都要自己在建表里给默认值**——
少一个「`NOT NULL` 又没 `DEFAULT`」的列，插入就报 `Field 'xxx' doesn't have a default value`。

**逻辑删除 / 乐观锁**：`BaseEntity` 里**已经删掉**了，需要时按实体单独加。
逻辑删除只需在字段上标 `@TableLogic`（删除变 UPDATE，查询自动追加条件）；乐观锁还要在
`MybatisPlusConfig` 里补回 `OptimisticLockerInnerInterceptor`。
⚠️ `sys_user` 的 `deleted` 是**反的**（1=正常 0=已删），必须写 `@TableLogic(value = "1", delval = "0")`，
只写 `@TableLogic` 会让删除逻辑整个反过来。

**分页**：查询条件继承 `common/dto/PageQuery`，别重复声明 pageNum/pageSize。
Service 里 `page(query.toPage(), wrapper)`，返回 `PageResult.of(page, XxxConverter::toVO)`。
`PageQuery.MAX_PAGE_SIZE`(500) 与 `MybatisPlusConfig` 的 `setMaxLimit` 同源，改一处两处都变。
`BlockAttackInnerInterceptor` 挡全表更新/删除，别摘。

**权限**：兜底是 `anyRequest().authenticated()`，匿名白名单是 `SecurityConfig.PUBLIC_PATHS`
（**应含** auth/login|register|refresh|logout、actuator/health|info、/error）和 `DOC_PATHS`。
⚠️ **新加的 auth 接口必须同时进白名单**——`register` 漏加过一次：不在清单里就走
`authenticated()`，可注册的人手里本来就没有 token，于是死锁（没账号→登不了→拿不到 token）。
⚠️ 除 auth 之外，加接口必须写显式 `@PreAuthorize`，别靠兜底——兜底只是「登录即可」。
`/api/users` 查询接口曾经就靠兜底，任何账号都能翻出全库邮箱手机号。

**认证**：JWT HS256，`app.jwt.secret` ≥ 32 字节（构造时校验，不足启动失败）。
access 30m / refresh 7d。refresh 带 `jti` 轮换（Redis Lua CAS，旧的失效）；
登出把 access 的 `jti` 加黑名单；改状态/角色按 userId 整体撤销。
`TokenStore` 读 Redis 异常**按已撤销处理（fail-closed）**，别改成 fail-open。
⚠️ 撤销那三条能力**目前是空转**：`refresh`/`logout`/改密码接口都还没写，
所以现在**没有任何地方调过 `blacklist` / `rotateRefreshToken` / `revokeUserTokens`**。
JWT 签出去就撤不掉，这直接导致「已知限制」第 7 条。

**测试**：纯 Mockito，不启 Spring。⚠️ Mockito 对未打桩的 `int` 返回 0，写路径用例里容易被
误判成「操作没生效」而去翻业务代码——必须显式 `thenReturn(1)`。

**风格**：源码注释已剥光，**新代码不写注释**。4 空格，LF（`.editorconfig` + spotless
`lineEndings: UNIX`，不指定 Windows 会全改 CRLF）。spotless 刻意没开完整格式化器，别顺手开。

## Flyway

- **已执行过的迁移一个字都不能改**（校验和 mismatch → 启动失败）。⚠️ **校验和算内容，改注释也会变**，已踩过
- **只增不减**，不写 DROP TABLE / DROP COLUMN
- 撞版本号就改名（校验和不算文件名）
- ⚠️ `baseline-version: 1`：库存量非空又被打过基线后，**V1 整份跳过且不报错**。
  `sys_user` 的 V1 已成功执行，**内容就此冻结**——动它必炸，只能重建库或改走 V2
- ⚠️ **别手工删 `flyway_schema_history` 里的行**（已踩过）：表还在、账本没了 → Flyway 重跑 V1 →
  `CREATE TABLE` 撞「表已存在」→ 记一条 `success=0`，之后每次启动都卡在这
- `baseline-on-migrate: true` 的代价是连错库不再报错——改 `datasource.url` 库名时人工确认一次
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
7. `refresh`/`logout` 没写：refresh token 发出去收不回来，**登出是假的**（前端清了本地
   token，服务端那条 access 照样有效到过期）；改密码/封号也没法撤销已发的令牌
8. `sys_user` 的 `uk_phone` 不含 `deleted`，软删掉的手机号**永久占位**，不能重新注册

## 上线前

`JWT_SECRET` 换强密钥 · CORS 改具体域名 · 关 springdoc ·
删 `application-dev.yml` 的 `log-impl`（打印 SQL 拖性能且泄露数据）· MySQL 密码走环境变量 ·
库账号非 root 但**必须给 CREATE/ALTER/INDEX**（Flyway 要 DDL）· 确认 `clean-disabled: true` ·
部署顺序「先数据库、后应用」· MinIO 换掉 minioadmin、桶策略只给 `GetObject`
（**别给 ListBucket**，否则枚举桶就拿到全部文件名）· 配 `MINIO_PUBLIC_URL` 并确认外网可达 ·
上传接口挂限流。
