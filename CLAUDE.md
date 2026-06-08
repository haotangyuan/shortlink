# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**开始任何开发工作前，必须先阅读 [doc/rules/开发者准则.md](doc/rules/开发者准则.md)。**

## Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/shortlink-all-1.0-SNAPSHOT.jar

# Docker
docker-compose up -d

# Frontend dev
cd frontend && npm install && npm run dev  # http://localhost:5173/app
```

Prerequisites: Java 21, Maven 3.6+, MySQL 8.0+, Redis 7.0+.

Database init: `mysql -u root -p db_shortlink < link.sql`

## Architecture

High-performance URL shortener. Spring Boot 3 backend + React frontend.

### Core Flow

```
短链接创建:  POST /api/short-link/v1/create → 号段分配 → 仿射变换 → Base62 → 6位短码
短链接跳转:  GET /{短码} → Caffeine → Redis → Bloom Filter → DB → 302 重定向
访问统计:    跳转 → XADD Redis Stream → Consumer Group → Lua(PFADD) → 异步入库
```

### Key Classes

| Class | Role |
|---|---|
| `LinkService` / `LinkServiceImpl` | 短链接 CRUD、跳转、缓存管理 |
| `ShortCodeUtil` | 号段 + 仿射置换 + Base62 短码生成 |
| `ShortCodeBootstrap` | 启动时初始化号段参数（a, b, N） |
| `LinkStatsService` | 访问统计查询 |
| `StatsConsumer` | Redis Stream 消费统计消息，Lua 脚本计算 UV/UIP |
| `GroupService` | 短链接分组管理 |
| `UserService` | 用户注册、登录、Token 管理 |
| `RecycleBinService` | 回收站管理 |
| `Result<T>` | 统一 API 响应包装 |
| `GlobalExceptionHandler` | 全局异常处理 |

### Sharding

ShardingSphere 按 `gid` 哈希分 16 张表：`t_link_0` ~ `t_link_15`、`t_group_0` ~ `t_group_15`。

配置文件：`src/main/resources/shardingsphere-config.yaml`

### Multi-Level Cache

```
L1 Caffeine (10min) → L2 Redis → L3 Bloom Filter → L4 空值缓存 (30min)
```

缓存未命中时使用 `Caffeine + ReentrantLock` 本地锁，避免分布式锁尾延迟。

### Message Queue

Redis Stream + Consumer Group 异步处理统计。`StatsConsumer` 消费后通过 Lua 脚本原子计算 HyperLogLog 增量，再批量入库。定时任务扫描 Pending 消息自动重试。

### Authentication

- **Admin 接口** (`/api/short-link/admin/v1/*`)：Session Token，`Authorization: Bearer <token>`
- **Core 接口** (`/api/short-link/v1/*`)：API Token（后台创建）
- **短链跳转** (`/{短码}`)：无需认证

## Environment Variables

See `.env.example`. Key ones:
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` — MySQL
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` — Redis
- `SERVER_PORT` — 默认 8068

## Conventions

- Lombok everywhere (`@Slf4j`, `@RequiredArgsConstructor`, `@Data`)
- MyBatis-Plus for DB access (`BaseMapper`, `LambdaQueryWrapper`)
- 统一响应 `Result<T>`，错误码 `ErrorCode` 枚举
- DTO/VO 分层：`dto/req/` 请求参数，`vo/` 响应视图
- 中文注释
- Lua 脚本放在 `src/main/resources/lua/`
