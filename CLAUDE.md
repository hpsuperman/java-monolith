# CLAUDE.md

给 Claude 的项目速览。**深度细节看 `README.md`**，这份只记「改代码时会踩什么」。

## 这是什么

Spring Boot 3 单体服务脚手架。75 个 Java 文件 / 约 4200 行（主 2754 + 测试 1447）。
auth 登录、user 用户 CRUD、material 物料 CRUD、file 文件上传、demo MQ 示例，
五个模块都是可运行的真实实现，不是占位符。

JDK 17 · Spring Boot 3.5.16 · MyBatis-Plus 3.5.17 · MySQL 8 · Redis 7 · RocketMQ 5 ·
MinIO 8.5.17 · Flyway · springdoc 2.9.1 · jjwt 0.13.0。版本坑都写在 pom.xml 注释里，升级前先读。

**仓库是公开的**：`github.com/hpsuperman/java-monolith`。提交前必查有没有真实凭据。
`.run/` 里有明文数据库密码，靠 `.gitignore` 挡着——**那条规则不能删**。

## 命令

```bash
docker compose up -d          # mysql / redis / rocketmq / minio（含建桶）
mvn spring-boot:run           # 端口 7070
mvn test                      # 103 个纯 Mockito 单测，不连中间件，任何机器都能跑
mvn verify                    # 额外跑 spotless 检查

mvn spotless:apply            # 只开无损规则：删无用 import、去行尾空白、补末尾换行
```

IDEA 直接跑 `.run/MonolithApplication.run.xml`（含**明文密码**，已 gitignore，分享前换掉）。

Swagger http://localhost:7070/swagger-ui.html · 默认账号 `admin` / `admin123`（仅当不存在时创建）。

## 代码地图

```
src/main/java/com/hpsuperman/monolith/
├── common/                    基础设施，不依赖 modules（一处例外见下）
│   ├── dto/PageQuery          分页入参基类，查询条件继承它
│   ├── config/                Security MybatisPlus Redis Cors Jackson OpenApi WebMvc
│   │                          MinioProperties MinioConfig
│   ├── security/              JwtTokenProvider JwtAuthenticationFilter TokenStore
│   │                          LoginUser SecurityUtils BearerTokenResolver
│   ├── entity/BaseEntity      id deleted version createTime updateTime createBy updateBy
│   ├── result/                Result PageResult ResultCode
│   ├── exception/             BizException GlobalExceptionHandler
│   ├── handler/               MyMetaObjectHandler（自动填充）
│   └── bootstrap/             AdminUserInitializer ← 反向依赖 modules.user
└── modules/                   auth · user · material · file · demo
    └── <模块>/                controller · service(+impl) · mapper · entity · dto
                               file 模块没有 mapper/entity——不入库，只往 MinIO 转存

src/main/resources/db/migration/   V1__init_user.sql · V2__add_material.sql
src/test/java/...                  10 个测试类，纯 Mockito，不起 Spring
```

**架构硬约束**：按领域分包（不是按技术层平铺），为了将来拆服务时整个包能搬走。
`common` 不依赖 `modules`，模块之间只通过 Service 接口调用。

> 已知违背：`common/bootstrap/AdminUserInitializer` 反向依赖 `modules.user`。
> 修法是挪进 `modules/user` 或抽 `modules/bootstrap`，但会改默认管理员的装配位置，暂未动。

## 加一个新模块

照 `material` 抄，它是最近的样板：`entity` 继承 `BaseEntity` → `mapper extends BaseMapper<T>` →
`service` 接口 + `impl extends ServiceImpl<XxxMapper, Xxx>` → `dto`（Request / VO / Converter）→
`controller`。**分页查询条件继承 `common/dto/PageQuery`**，别重复声明 `pageNum`/`pageSize`。
建表写 `V3__add_xxx.sql`，重启应用即可，不用手工跑 SQL。

Mapper 不用逐个 `@Mapper`，`MybatisPlusConfig` 上有 `@MapperScan("...modules.**.mapper")`。

## 改代码前必须知道的

### 响应码：业务失败返回 HTTP 200

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

刻意如此：让前端统一判 `success`。认证/权限类必须让 HTTP 状态准确，否则前端跳不了登录页。
**`body.code` 区分不了失败原因**——`BIZ_ERROR`、`DATA_CONFLICT`、系统异常都是 500，原因只在 `message`。
抛业务异常用 `BizException`；需要 409 语义就用 `BizException.throwIf(cond, ResultCode.DATA_CONFLICT, msg)`。

### MyBatis-Plus

- **实体必须继承 `BaseEntity`**，子类**必须显式写** `@EqualsAndHashCode(callSuper = true)` 和
  `@ToString(callSuper = true)`——Lombok 默认 `false`，漏了会把父类字段整个排除掉**且不报错**
  （id 不同的两个实体会 `equals` 相等）。`User` 上还要 `exclude = "password"`。`BaseEntityTest` 盯着。
- `update-strategy: not_null` —— **传 null 表示不修改，不是置空**；要清空传空字符串。
  例外：`roles` 传空数组 = 清空；物料数字字段传 0 = 清空，且清不回 NULL。
- 逻辑删除 `deleted`，所有 `lambdaQuery` 自动追加 `deleted = 0`
- 乐观锁 `@Version`：更新前 `update.setVersion(existing.getVersion())`，失败返回 `false` → 转 409
- 分页必须走已注册的插件；`BlockAttackInnerInterceptor` 防全表更新删除
- **分页入参基类是 `common/dto/PageQuery`**，Service 里 `page(query.toPage(), wrapper)`。
  单页上限是 `PageQuery.MAX_PAGE_SIZE`（500），`MybatisPlusConfig` 的 `setMaxLimit` 引用同一个常量
- 返回侧 `common/result/PageResult`，`of(page, XxxConverter::toVO)` 一步转 VO
- `deleted` / `version` **不在自动填充里**，靠建表脚本 `NOT NULL DEFAULT 0` 兜底——建表别省

### 权限

在 controller 方法上写显式 `@PreAuthorize`。

| 接口 | 权限 |
|---|---|
| `POST /api/auth/{login,refresh,logout}` | 匿名 |
| `GET /api/auth/me` | 任意登录用户 |
| `/api/users/**`（**含两个查询接口**） | `hasRole('ADMIN')` |
| `/api/materials/**`（含写接口） | `hasAnyRole('ADMIN','USER')` |
| `POST /api/files/upload` | `hasAnyRole('ADMIN','USER')` |
| `/actuator/health` `/info`、文档 | 匿名 |
| 其余 | `anyRequest().authenticated()` |

`/api/users` 的查询接口也限 ADMIN——它们曾经只要「已登录」，于是任何账号都能翻出全库用户邮箱手机号。
`/api/materials` **刻意相反**，全部对 USER 开放（物料无个人信息），且写的是显式注解而不是靠兜底，
免得后来者以为和用户模块一样是漏加了。**加接口时别只依赖 `anyRequest()` 兜底。**

### 文件上传（MinIO）

`FileService` 是唯一入口，返回 `FileUploadVO`（`objectKey` / `url` / `originalName` / `size` /
`contentType`）。**两个已定的取舍，改之前先想清楚**——换掉任何一个都是重写：

- **中转上传**，不是前端直传预签名 URL。校验和改名集中在服务端一处
- **bucket 公开读**，URL 永久可存库。要私有就得改存 `objectKey`、每次查询再签发临时 URL

服务端三重校验：扩展名白名单 → **文件头魔数** → 大小上限。`filename` 和 `Content-Type`
一律不采信（前者只用来取扩展名，**绝不进存储路径**；objectKey 是 `yyyy/MM/dd/<雪花ID>.<扩展名>`
重新生成的，路径穿越碰不到）。

⚠️ **白名单里有已知签名的扩展名才走魔数校验**。往 `app.minio.allowed-extensions` 加 pdf/docx 时，
记得同步往 `FileServiceImpl.SIGNATURES` 补签名，否则那类文件只靠白名单挡。

⚠️ **大小上限只有一个来源**：`spring.servlet.multipart.max-file-size` 写成
`${app.minio.max-file-size}`，别改成不同字面量——容器那道总是先拦，业务校验就永远不执行。
超限由 `GlobalExceptionHandler` 转成 413。

⚠️ **`app.minio.endpoint` 与 `app.minio.public-url` 是两回事**：前者是应用连 MinIO 用的，
后者是拼进返回 URL 给浏览器的。应用在容器里时 endpoint 得写 `http://minio:9000`，
那个主机名浏览器解析不了，public-url 必须填成浏览器能访问的地址——否则上传成功但图片全裂。

**应用不依赖 MinIO 可用**：桶由 compose 的 `minio-init` 建，不在启动时建，`MinioClient`
构造也不发请求。MinIO 挂了应用照常起，只有上传接口报业务异常。**别在启动时建桶。**

### 认证链路

JWT HS256，`app.jwt.secret` 至少 32 字节（`JwtTokenProvider` 构造时校验，不足直接启动失败）。
access 30m / refresh 7d。refresh token 带 `jti`，**轮换**（Redis Lua 脚本 CAS，旧的一次性失效）；
登出把 access 的 `jti` 加黑名单；改状态/角色后按 userId 整体撤销。
`TokenStore` 读到 Redis 异常时**按已撤销处理（fail-closed）**——别改成 fail-open。

### 自操作守卫

不能禁用自己、不能摘掉自己的 ADMIN（`guardSelfUpdate`）、不能删除自己。
**但挡不住 A 禁 B、B 禁 A**，见已知限制。

### 测试

纯 Mockito，不启 Spring、不连 MySQL/Redis/MQ。覆盖的是「错了也未必报错」的逻辑。

⚠️ **Mockito 对未打桩的 `int` 返回 0**，在写路径用例里会被当成「乐观锁未命中」而误判成 bug。
这类用例必须显式 `thenReturn(1)`。

### 代码风格

源码注释已被剥光，**新代码也不要写注释**。缩进 4 空格，行尾 LF（`.editorconfig` + spotless 的
`lineEndings: UNIX`——不指定的话 Windows 上会把全部文件改写成 CRLF）。
spotless 刻意没开完整格式化器，别顺手开。

## 已知限制（是刻意的取舍，别「顺手修」）

1. 两个管理员可以互相锁在门外
2. `common` 反向依赖 `modules`（`AdminUserInitializer` 一处）
3. 角色是逗号分隔串，不是标准 RBAC 三张表
4. Redis 挂掉时携带令牌的请求一律 503，登录/刷新/登出全失败
5. `biz_material.stock` 可被 PUT 改成任意值，无出入库流水。乐观锁防的是并发写，**不是超卖**
6. 物料删除后编码不可复用（唯一键不含 `deleted`，查重却带 `deleted = 0`）→ INSERT 撞唯一键 → 409
7. 启动强依赖数据库可达（Flyway 在启动阶段跑）
8. 上传的文件不自动清理——换图或删物料，旧对象永远留在 MinIO。没有引用计数也没有清理任务
9. 上传没做病毒扫描和图片二次编码。魔数校验只证明「这确实是张 PNG」，不证明「这张 PNG 安全」

## Flyway 红线

- **已执行过的迁移文件一个字都不能改**。存校验和，改动让所有跑过的库启动即失败
- **只增不减**，不写 `DROP TABLE` / `DROP COLUMN`。下线字段分两个版本走
- 撞版本号就改名（校验和算内容不算文件名）
- `baseline-on-migrate: true` 是给存量库用的，代价是连错库不再报错——改 `datasource.url` 库名时人工确认一次
- 本地想重来就 `DROP DATABASE` 重建，别手工 DROP 单表（基线已记下，救不回来）

## 上线前

`JWT_SECRET` 换强密钥 · `app.init-admin.enabled: false` · CORS 改具体域名 ·
关 springdoc · 删 `application-dev.yml` 的 `log-impl`（打印 SQL 拖性能且泄露数据）·
MySQL 密码走环境变量 · 库账号非 root 但**必须给 `CREATE`/`ALTER`/`INDEX`**（Flyway 要 DDL）·
确认 `clean-disabled: true` · 部署顺序「先数据库、后应用」·
MinIO 换掉 `minioadmin` 默认账号、桶策略只给 `GetObject`（**别给 `ListBucket`**，否则枚举桶就拿到全部文件名）·
配 `MINIO_PUBLIC_URL` 并确认外网能打开 · 上传接口挂限流（一次请求就是一次磁盘写入）。
