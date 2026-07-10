## ShortLink 项目深度分析报告

### 项目概述

ShortLink 是一个高性能短链接服务，基于 Spring Boot 3 + Redis + ShardingSphere 构建，采用 Java 21 开发。项目实现了完整的短链接生命周期管理，包括生成、跳转、统计、限流和分库分表，同时集成了 MCP（Model Context Protocol）以支持 AI 工具调用。

---

### 一、技术栈总览

| 层级 | 技术选型 |
|------|----------|
| 语言 | Java 21 |
| 后端框架 | Spring Boot 3.1.10 |
| 前端框架 | React 19 + TypeScript 6 + Vite 8 |
| 样式 | Tailwind CSS v4 |
| ORM | MyBatis Plus 3.5.5 |
| 数据库 | MySQL 8.0 + ShardingSphere 5.3.2（16 表分片） |
| 缓存 | Caffeine 3.1.8 + Redis + Redisson Bloom Filter |
| 消息队列 | Redis Stream + Consumer Group |
| 限流 | Guava RateLimiter + Redis Lua 滑动窗口 |
| IP 定位 | ip2region 离线数据库 |
| API 文档 | SpringDoc OpenAPI + Scalar |
| AI 集成 | MCP Java SDK (mcp-spring-webmvc 0.11.3) |
| 图表 | Recharts 3.8 |

---

### 二、项目结构

```
shortlink/
├── src/main/java/dev/haotangyuan/shortlink/
│   ├── ShortLinkApplication.java         # 启动入口
│   ├── common/                           # 基础设施层
│   │   ├── config/                       # 13 个配置类（数据源、缓存、限流、MCP等）
│   │   ├── web/                          # 5 个过滤器（认证、限流、风控、传输）
│   │   ├── convention/                   # 统一异常体系与返回码
│   │   ├── constant/                     # Redis Key、Link、User 常量
│   │   ├── database/                     # BaseDO 通用字段
│   │   ├── enums/                        # 有效期类型枚举
│   │   ├── serialize/                    # 手机号脱敏序列化器
│   │   └── biz/user/                     # 用户上下文、分组归属校验
│   ├── controller/
│   │   ├── admin/                        # 7 个 Admin Controller（Session Token 认证）
│   │   └── core/                         # 6 个 Core Controller（API Token 认证）
│   ├── service/                          # 7 个 Service 接口 + 7 个 Impl
│   ├── dao/
│   │   ├── entity/                       # 13 个 DO 实体
│   │   └── mapper/                       # 13 个 MyBatis Plus Mapper
│   ├── dto/                              # 请求 DTO（req/）+ 内部业务 DTO（biz/）
│   ├── vo/                               # 16 个响应视图对象
│   ├── mq/
│   │   ├── producer/                     # Redis Stream 生产者
│   │   ├── consumer/                     # 消费者 + 统计持久化器
│   │   ├── idempotent/                   # 三层幂等处理器
│   │   └── task/                         # Pending 消息恢复 + Stream 清理
│   ├── mcp/                              # MCP Server（SSE 端点）
│   ├── toolkit/                          # 短码生成器、IP 地理位置客户端
│   └── initialize/                       # Redis Stream 初始化任务
├── frontend/                             # React SPA
│   ├── src/
│   │   ├── api/                          # client.ts + admin.ts + core.ts + types.ts
│   │   ├── app/                          # router.tsx + providers.tsx
│   │   ├── components/                   # layout/ + ui/ 通用组件
│   │   ├── features/                     # 8 个业务模块
│   │   └── store/                        # Auth Context
│   └── dist/                             # 生产构建产物
├── docs/                                 # Superpowers 设计、计划和验证记录
├── notebook/                             # 项目文档与开发记录
├── scripts/                              # 构建脚本
├── link.sql                              # 完整数据库 DDL（~60KB）
├── docker-compose.yml                    # 三容器编排
├── Dockerfile                            # 多阶段构建
└── pom.xml                               # Maven 配置
```

---

### 三、核心架构设计

#### 3.1 短码生成算法：号段 + 仿射置换 + Base62

这是项目最精巧的设计之一，采用三阶段流水线：

**第一阶段：分布式号段分配。** 通过 Redis `INCRBY` 一次获取 10000 个序号（可配置），存入本地号段池。每个 JVM 实例持有一个活跃号段 `[start, end]`，通过 `AtomicLong` 游标进行无锁 CAS 分配。当号段剩余 20% 时，触发 `CompletableFuture` 异步预取下一段，确保分配过程零阻塞。

**第二阶段：仿射密码置换。** 将线性序号 `i` 映射为 `y = (A*i + B) mod N`，其中 `N = 62^6 ≈ 568 亿`。参数 A 必须与 62 互素（启动时校验）。这是一个双射（一一对应），保证序号唯一则短码唯一，同时让连续序号变得不可预测。

**第三阶段：定长 Base62 编码。** 将置换后的值编码为恰好 6 位 `[0-9A-Za-z]` 字符。

跳转链路不使用本地号段游标判断短码是否存在：多实例分别持有不连续号段，本地游标无法代表其他实例已经创建的短码，直接据此否定会产生错误 404。

#### 3.2 多级缓存跳转（Redirect Flow）

跳转链路采用纵深防御策略，逐级降低延迟和穿透风险：

| 层级 | 组件 | 作用 | TTL |
|------|------|------|-----|
| L1 | Caffeine | 进程内热点缓存，零网络开销 | 10min |
| L2 | Redis | 分布式缓存，多实例共享 | 跟随有效期 |
| L3 | Bloom Filter | 创建时写入，快速否定不存在的短码 | 持久化 |
| L4 | 空值缓存 | 缓存"不存在"的结果，防穿透 | 30min |
| L5 | DB 查询 | 最终兜底：t_link_goto → t_link | — |

关键的并发控制决策：缓存未命中时使用 **本地 `ReentrantLock`**（由 Caffeine 管理的 per-key 锁池），而非分布式锁。这避免了在跳转热路径上引入 Redis 往返延迟。锁获取后执行 double-check（再次检查 L1/L2），减少重复查询。

#### 3.3 异步统计管道（Redis Stream）

统计系统通过 Redis Stream 将跳转与持久化完全解耦：

**生产端：** 跳转完成后 `XADD` 写入统计消息（包含 UV cookie、IP、OS、浏览器、设备信息），立即返回 302。

**消费端：** `StreamListener` 消费消息，执行以下流水线：
1. 幂等检查（Redis SETNX，2 分钟 TTL）
2. Lua 脚本原子计算 HLL 增量（PFADD + PFCOUNT 差值），得到 UV/UIP 增量
3. 首次访问判定（INSERT IGNORE 去重表）
4. IP 地理位置查询（ip2region）
5. 访问日志写入（messageId 唯一索引作为 DB 级幂等兜底）
6. 六维统计表 upsert（地区、OS、浏览器、设备、网络、小时级汇总）
7. 链接聚合计数器更新（乐观 GID 缓存 + 读锁降级）

**可靠性保障：** 每 30 秒执行 Pending 消息恢复任务，通过 `XAUTOCLAIM` 认领空闲超过 2 分钟的消息重新消费。采用游标分页遍历 PEL，每批最多 200 条。

**HLL 滚动日活：** 使用两个交替的 HLL key（`epochDay % 2`），实现每日唯一计数的滚动重置，24 小时 TTL 自动清理。

#### 3.4 限流策略

**进程级限流：** 使用 Guava `RateLimiter`（令牌桶，smooth-bursty 模式），按端点类型独立配置：

| 端点类型 | QPS | 超时 |
|----------|-----|------|
| 跳转 `GET /{shortUri}` | 1000 | 5ms |
| 创建（单条） | 500 | 20ms |
| 创建（批量，5 permits） | 500 | 20ms |
| 统计查询 | 100 | 50ms |

过滤器优先级设为 `HIGHEST_PRECEDENCE`，在认证之前拦截超额流量。跳转被限流时返回用户友好的 HTML 429 页面（含重试按钮），API 被限流返回 JSON。

**IP 级风控：** 独立过滤器限制单 IP 在 5 秒窗口内最多 5 次访问，基于 Redis Lua 滑动窗口实现。

#### 3.5 认证授权模型

项目采用双 API 架构：

**Admin API (`/api/short-link/admin/v1/*`)：** Session 认证。登录时生成 UUID Token，存入 Redis（`session:{token} → username`），30 分钟滑动过期（每次请求刷新）。支持匿名创建（无 Token 时降级为 `PUBLIC_USERNAME`）。

**Core API (`/api/short-link/v1/*`)：** API Token 认证。Token 经 SHA-256 哈希后作为 Redis Key 查找用户名，**不以明文存储**。长期有效，适合程序化调用。

**授权：** `GroupOwnershipVerifier` 校验用户是否拥有目标分组。Redis Set `user_gids:{username}` 维护用户分组索引，登录时通过 Lua 脚本原子重建。

---

### 四、数据库设计

#### 4.1 表结构概览

数据库 `db_shortlink` 包含 11 个逻辑表，其中 3 个做了 16 表分片：

| 逻辑表 | 分片数 | 分片键 | 用途 |
|--------|--------|--------|------|
| `t_user` | 1 | — | 用户账户 |
| `t_group` | 16 | `username` | 链接分组 |
| `t_group_unique` | 1 | — | GID 全局唯一性 |
| `t_link` | 16 | `gid` | 核心短链接记录 |
| `t_link_goto` | 16 | `full_short_url` | 跳转路由（短 URL → GID 映射） |
| `t_link_access_logs` | 1 | — | 原始访问日志 |
| `t_link_access_stats` | 1 | — | 小时级 PV/UV/UIP 汇总 |
| `t_link_browser/os/device/network/locale_stats` | 各 1 | — | 六维日统计 |
| `t_link_stats_today` | 1 | — | 今日实时统计 |
| `t_api_token` | 1 | — | API 令牌 |
| `t_link_first_visit` | 1 | — | 首次访问去重（UV 计数） |

#### 4.2 分片策略

分片键设计精巧，确保高频查询只命中单个分片：

- **`t_link` 按 `gid` 分片：** 同组链接在同一分片，分组内列表查询不会跨片。
- **`t_link_goto` 按 `full_short_url` 分片：** 跳转时只有短 URL，按此分片可单片查找 GID，再路由到 `t_link` 的正确分片。
- **`t_group` 按 `username` 分片：** 用户的所有分组在同一片，列表查询不跨片。

算法为 `HASH_MOD`（`hash(column) % 16`），由 ShardingSphere JDBC 驱动透明执行。

#### 4.3 关键设计模式

**软删除 + 可重用唯一约束：** `t_link` 的唯一索引是 `(full_short_url, del_time)`，`del_time` 默认为 0。删除时设为删除时间戳，释放短 URL 供重建使用。

**两阶段跳转查找：** `短 URL → t_link_goto（找 GID）→ t_link（找原始 URL）`。因为跳转时不知道 GID，需要 goto 表做路由中转。

**反规范化计数器：** `t_link` 直接存储 `total_pv/total_uv/total_uip`，由消费者异步同步，避免跳转时 join 统计表。

**字段加密：** ShardingSphere 的 `!ENCRYPT` 规则对 `t_user` 的 `phone` 和 `mail` 字段做 AES 透明加密。

---

### 五、前端架构

#### 5.1 技术栈

前端是一个现代化的 React 19 SPA，使用 Vite 8 构建，Tailwind CSS v4 样式，react-router-dom v7 路由，TanStack React Query v5 管理服务端状态。表单使用 react-hook-form + Zod 校验，图表使用 Recharts。

#### 5.2 路由结构

公开路由（PublicLayout）包括首页（公开创建短链）、登录、注册。受保护路由（ProtectedRoute + DashboardLayout）包括仪表盘总览、分组管理、链接列表/创建/编辑、回收站、数据分析、API Token 管理、个人设置。

#### 5.3 状态管理

不使用 Redux/Zustand，而是通过 React Context（Auth 状态）+ TanStack React Query（服务端状态）+ URL Search Params（筛选/分页）组合管理，简洁而够用。

#### 5.4 API 层

分为 `admin.ts`（管理端 API）和 `core.ts`（开发者 API），底层由 `client.ts` 统一处理认证头注入、401 自动清理、`ApiResult<T>` 响应解包。

---

### 六、部署与 DevOps

#### 6.1 Docker Compose 三容器编排

- **MySQL 8.0：** 自动导入 `link.sql`，配置 `utf8mb4`、`STRICT_TRANS_TABLES`、`innodb-buffer-pool-size=256M`。
- **Redis 7.0 Alpine：** AOF 持久化，密码保护。
- **App：** 依赖 MySQL/Redis 健康检查通过后启动，G1 GC，最大堆 1024MB。

所有服务使用 `.env` 管理密码，通过 `shortlink-network` 桥接网络通信。健康检查覆盖全部三个容器。

#### 6.2 MCP 集成

通过 MCP Java SDK 暴露 SSE 端点 (`/api/mcp`) 和消息端点 (`/api/mcp/message`)，注册 `createShortLink` 工具，允许 AI 助手创建短链接（3 天有效期，公开用户上下文）。

---

### 七、设计亮点与改进建议

#### 亮点

1. **号段 + 仿射 + Base62 短码生成器** — 无锁 CAS 快路径 + 异步预取，亚微秒级生成，碰撞零概率，不可预测。
2. **四层缓存纵深防御** — Caffeine + Redis + Bloom Filter + 空值缓存，本地锁避免分布式延迟放大。
3. **三层幂等保障** — Redis SETNX 快判 + DB 唯一索引兜底 + PEL 恢复补偿，实现 at-least-once 语义。
4. **Lua 脚本原子操作** — HLL 增量计算、GID 索引重建、Stream XAUTOCLAIM 均通过 Lua 保证原子性。
5. **分片键设计** — 高频查询（分组内列表、跳转路由、用户分组列表）均命中单片。

#### 改进建议

1. **`HashUtil` 遗留代码：** MurmurHash 方式是早期方案的残留，当前已不被 `ShortCodeUtil` 使用，可以考虑清理。
2. **批量创建串行执行：** `batchCreateLink` 循环调用 `createLink` 并逐条 try-catch，单条慢会阻塞整批。可引入并行执行（`CompletableFuture.allOf`）。
3. **进程级限流的局限：** Guava `RateLimiter` 是单 JVM 的，多实例部署时有效限流倍数 = 实例数 × 单实例 QPS。可考虑 Redis 分布式限流做集群级保障。
4. **用户更新缺少权限校验：** `UserServiceImpl.updateByUsername` 有 TODO 注释需要补充权限校验。
5. **时区硬编码：** `Asia/Shanghai` 在多处硬编码，如需国际化需参数化。
6. **pom.xml 重复依赖：** `mysql-connector-j`、`spring-boot-starter-jdbc`、`spring-boot-starter-data-redis`、`redisson`、`shardingsphere`、`fastjson2` 各出现了两次，需要去重。
7. **统计表未分片：** 访问日志和六维统计表目前未分片，随着数据量增长可能成为瓶颈，可考虑按 `full_short_url` 或时间维度分片。
