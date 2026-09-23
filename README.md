# java-monolith

Java 单体服务脚手架。Spring Boot 3 + MyBatis-Plus + MySQL + Redis + RocketMQ + Spring Security(JWT)。

## 技术选型

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3 的最低要求 |
| Spring Boot | 3.5.16 | 3.x 线最新稳定版 |
| MyBatis-Plus | 3.5.17 | 另需 `mybatis-plus-jsqlparser`，3.5.9 起已拆为独立依赖 |
| MySQL | 8.0 | 驱动为 `com.mysql:mysql-connector-j` |
| Flyway | 11.7.2 | 建表与版本管理。版本由 `spring-boot-starter-parent` 管理，pom 里不写 `<version>`；MySQL 自 Flyway 10 起需额外引 `flyway-mysql` |
| Redis | 7 | 用于刷新令牌与访问令牌黑名单 |
| RocketMQ | 5.x | starter 版本 2.3.6 |
| springdoc-openapi | 2.9.1 | **2.x 线对应 Spring Boot 3**，3.x 线是给 Spring Boot 4 的 |
| jjwt | 0.13.0 | 使用 0.12+ 新 API |

> Spring Boot 4.x 已发布，但 MyBatis-Plus 的 `-spring-boot3-` starter 与 springdoc 2.x 都绑定
> Spring Boot 3 大版本，升级需要整条链路一起动，暂不跟进。

## 快速开始

```bash
docker compose up -d          # mysql / redis / rocketmq
mvn spring-boot:run
```

- 接口文档 http://localhost:7070/swagger-ui.html
- 健康检查 http://localhost:7070/actuator/health
- 默认管理员 `admin` / `admin123`（仅当 admin 不存在时创建）

**建表不用手工做**：Flyway 在启动时执行 `src/main/resources/db/migration/`，Docker 只负责建出空库。
不用 Docker 就先建库，表仍归 Flyway 管：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS monolith DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

连接默认 `127.0.0.1` / `root` / `root`，可用 `MYSQL_USER`、`MYSQL_PASSWORD`、`REDIS_HOST`、
`ROCKETMQ_NAME_SERVER` 覆盖。IDEA 直接跑 `.run/MonolithApplication.run.xml`（已配好本机密码；
含**明文密码**，已加入 `.gitignore`，分享前换成占位符）。不用 RocketMQ 就注释掉
`application-dev.yml` 里的 `rocketmq:` 并设 `app.mq.enabled: false`。

```bash
# 登录拿 token
curl -X POST http://localhost:7070/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 带 token 查用户列表
curl "http://localhost:7070/api/users?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <accessToken>"
```

> 不用 Docker 跑 RocketMQ（Windows 原生）有两个必踩的坑：必须用 JDK 8 启动，
> 以及 `.bat` 不能含中文注释。

## 目录结构

```
src/main/java/com/hpsuperman/monolith/
├── common/      通用能力：config / entity / exception / handler / result / security / bootstrap
└── modules/     业务模块：auth 登录 · user 用户 CRUD · material 物料 CRUD · demo MQ 示例

src/main/resources/db/migration/   建表脚本，应用启动时由 Flyway 按序执行
src/test/java/com/hpsuperman/monolith/   纯 Mockito 单测，不启 Spring、不需要中间件
```

按领域分包而不是按技术层平铺，是为了将来真要拆服务时整个包能直接搬走。因此有一条硬约束：
**`common` 不依赖 `modules`，模块之间只通过 Service 接口调用。**

> 已知例外：`common/bootstrap/AdminUserInitializer.java` 反向依赖了 `modules.user`。修法很简单
> （挪进 `modules/user`，或抽一个 `modules/bootstrap`），但会改变默认管理员的装配位置，暂未动。

## 关键约定

### 响应码

```json
{ "code": 200, "message": "成功", "data": { }, "success": true }
```

| 场景 | HTTP 状态 | body.code |
|---|---|---|
| 成功 | 200 | 200 |
| 业务失败（用户名已存在等） | **200** | 500 |
| 乐观锁冲突 | **200** | 500 |
| 唯一键冲突 | 409 | 500 |
| 参数校验失败 | 400 | 400 |
| 未认证 / 令牌过期 | 401 | 401 |
| 无权限 | 403 | 403 |
| 系统异常 | 500 | 500 |

业务异常返回 HTTP 200，让前端能统一判 `success`；认证/权限类则必须让 HTTP 状态码准确，
否则前端无法触发跳登录页。

⚠️ **`body.code` 区分不了失败原因**：`BIZ_ERROR` 与 `DATA_CONFLICT` 都取 500，与系统异常同码，
具体原因只在 `message` 里。前端只能判 `success`（等价于 `code == 200`）。

### 接口权限

| 接口 | 权限 |
|---|---|
| `POST /api/auth/login` `/refresh` `/logout` | 匿名 |
| `GET /api/auth/me` | 任意登录用户 |
| `/api/users/**`（**含两个查询接口**） | `ADMIN` |
| `/api/materials/**`（含写接口） | `USER` 或 `ADMIN` |
| `/actuator/health` `/info`、接口文档 | 匿名（生产环境文档已关闭） |
| 其余 | 登录即可 |

`/api/users` 的查询接口也限 `ADMIN`——它们曾经只要求「已登录」，于是任何普通账号都能翻出全库
用户的邮箱与手机号。`/api/materials` **刻意相反**，5 个接口全部对 `USER` 与 `ADMIN` 同等开放，
因为物料档案里没有个人信息；`MaterialController` 上写的是显式 `@PreAuthorize` 而不是留空靠兜底，
免得后来者以为和用户模块一样是漏加了注解。

### 数据库迁移（Flyway）

表结构定义在 `src/main/resources/db/migration/`，**应用启动时自动执行**，记录落在
`flyway_schema_history`。加一张表：新建 `V3__add_xxx.sql` → 重启应用 → 完事。
命名 `V<版本>__<描述>.sql`，版本与描述之间是**两个下划线**。

1. **已执行过的迁移文件一个字都不能改。** Flyway 存校验和，改动会让所有执行过它的库在启动时报
   `Migration checksum mismatch` 而**直接起不来**。要改结构就新建 V3。
2. **迁移只增不减。** 不要写 `DROP TABLE` / `DROP COLUMN`。真要下线一个字段分两次：
   先发一个让代码不再读写它的版本，确认无影响后再单独发一个迁移删列。
3. **多人协作会撞版本号。** 两人各写了 V3 时，后合并的改名成 V4。改名是安全的——
   校验和算的是文件内容，不含文件名。

存量库（老版本用 `db/schema.sql` 建的）**不需要清库**，启动一次应用即可：

| 你的库现在 | 启动前要做的事 |
|---|---|
| 只有 `sys_user` | **什么都不用做**，Flyway 打基线到 V1，只执行 V2 ✓ |
| 已有 `sys_user` + `biz_material` | **启动前**先 `DROP TABLE IF EXISTS biz_material;` |
| 库不存在 / 是空的 | 先建库（见上），V1、V2 自动执行 ✓ |

第二行为什么强调「启动前」：**MySQL 的 DDL 不是事务性的**，V2 失败后 `flyway_schema_history` 里会
留下一条 `success = 0`，之后**每次启动都被它挡住**。真发生了要两步恢复：

```sql
DROP TABLE IF EXISTS `biz_material`;
DELETE FROM `flyway_schema_history` WHERE version = '2' AND success = 0;
```

**本地想彻底重来**就整个库重建，不要手工 DROP 单张表——基线一旦记下，删表后重启是救不回来的
（Flyway 会说 `Schema is up to date`，应用直到真正用那张表时才报错）：

```sql
DROP DATABASE monolith;
CREATE DATABASE monolith DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

生产环境同样自动执行，代价是**数据库不可达则应用启动直接失败**（所以上线顺序必须是「先起数据库、
再起应用」），且库账号需要 `CREATE` / `ALTER` / `INDEX` 权限，与最小权限原则冲突。

### MyBatis-Plus

- **实体继承 `common/entity/BaseEntity`**，公共字段只有一处定义：`id` / `deleted` / `version` /
  `createTime` / `updateTime` / `createBy` / `updateBy`。MyBatis-Plus 靠反射遍历整条继承链收集
  带注解的字段，继承来的 `@TableId`、`@TableLogic`、`@Version`、`@TableField(fill = ...)` 一样生效。

  ⚠️ **子类必须显式写 `@EqualsAndHashCode(callSuper = true)` 与 `@ToString(callSuper = true)`。**
  Lombok 这两个的 `callSuper` **默认是 false**，漏了会把父类字段整个排除掉，而且**不报错**：
  id 不同的两个实体会 `equals` 相等，日志里也看不到主键。`User` 上还要写 `exclude = "password"`。
  这条由 `BaseEntityTest` 盯着。

- 分页插件已在 `MybatisPlusConfig` 显式注册（不注册则完全不生效）
- 逻辑删除：`deleted` 字段，查询自动追加 `deleted = 0`
- 自动填充：`createTime` / `updateTime` / `createBy` / `updateBy`。`deleted` / `version`
  **不在其中**，靠建表脚本的 `NOT NULL DEFAULT 0` 兜底
- 乐观锁：`@Version` 字段，更新时必须带上版本号；防全表更新删除靠 `BlockAttackInnerInterceptor`
- `update-strategy: not_null` —— **传 null 表示不修改，不是置空**；要清空字段请传空字符串。
  两个例外：`roles` 传空数组 = 清空角色；物料的数字字段传 0 = 清空，且**清不回 NULL**

### 分页

查询条件继承 `common/dto/PageQuery`，只需写自己模块的过滤字段，分页参数不用重复声明：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XxxQueryRequest extends PageQuery {
    private String name;
}
```

Service 里一行拿到 MyBatis-Plus 的 `Page`：

```java
Page<Xxx> page = page(query.toPage(), wrapper);
return PageResult.of(page, XxxConverter::toVO);
```

- `pageNum` 默认 1、`pageSize` 默认 10，入参校验是 `@Min(1)` + `@Max(PageQuery.MAX_PAGE_SIZE)`
- `PageQuery.MAX_PAGE_SIZE`（500）同时被 `MybatisPlusConfig` 的 `setMaxLimit` 使用，
  **改一处两处都变**，不会出现「注解写 1000、插件截到 500」的静默不一致
- 子类同样必须写 `@EqualsAndHashCode(callSuper = true)`（原因同实体，见上）

> 只封装了**入参**。返回侧是 `PageResult`，与 `PageQuery` 无关。

## 测试

```bash
mvn test
```

全部是**纯 Mockito 单测**：不启 Spring 上下文，不连 MySQL / Redis / MQ，在任何机器上都能直接跑通。
覆盖的是安全与一致性这类「错了也未必报错」的逻辑（JWT、令牌轮换、自我操作守卫、乐观锁、
空值语义、CORS 通配符、`BaseEntity` 的 `callSuper`）。**Mockito 对未打桩的 `int` 返回 0**，
在写路径的用例里会被当成「乐观锁未命中」而误判成代码有 bug，这类用例必须显式 `thenReturn(1)`。

## 已知限制

1. **两个管理员可以把彼此锁在门外**：自我操作守卫只挡「禁用自己」和「摘掉自己的 ADMIN」，
   挡不住 A 禁用 B、B 禁用 A
2. **`common` 反向依赖了 `modules`**：`AdminUserInitializer` 一处，见上
3. **角色是逗号分隔串**，不是标准的 user / role / user_role 表，正式项目请换掉
4. **认证链路依赖 Redis 可用性**：Redis 挂掉时携带令牌的请求一律 503，登录/刷新/登出也都失败
5. **`biz_material.stock` 可以被任意 PUT 改写成任意值**，与真实库存没有因果关系，本模块也没有
   出入库流水。乐观锁防的是「并发写」，**不是「超卖」**
6. **物料删除后编码不可复用**：唯一键不含 `deleted`，且查重走 `lambdaQuery` 会被追加
   `deleted = 0`，于是会在 INSERT 时撞唯一键，由全局异常处理转成 409
7. **应用启动依赖数据库可达**：Flyway 迁移在启动阶段执行，库连不上或校验和不匹配都会让应用
   **直接启动失败**

## 上线前检查清单

- [ ] `JWT_SECRET` 换成随机强密钥，通过环境变量注入
- [ ] `app.init-admin.enabled: false`，删除 `AdminUserInitializer`
- [ ] `app.cors.allowed-origins` 改成具体域名，`allow-credentials` 按需
- [ ] `springdoc.api-docs.enabled` / `swagger-ui.enabled` 置为 false（`application-prod.yml` 已配）
- [ ] 删掉 `mybatis-plus.configuration.log-impl`（生产打印 SQL 会拖慢性能并泄露数据）
- [ ] `management.endpoints.web.exposure.include` 保持最小集合，不要开 `*`
- [ ] MySQL / Redis 密码走环境变量或配置中心，不要提交到仓库
- [ ] 数据库账号不要用 root，但**必须授予 `CREATE` / `ALTER` / `INDEX`**——Flyway 要执行 DDL，
      权限给少了应用起不来
- [ ] 确认 `spring.flyway.clean-disabled` 仍为 `true`
- [ ] 确认部署顺序是「先数据库、后应用」
