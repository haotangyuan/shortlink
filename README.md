# ShortLink

高性能短链接服务，基于 Spring Boot 3 + Redis + ShardingSphere 构建。

## Features

- **短链接生成** - Redis 号段 + 仿射 Base62，6 位短码
- **302 跳转** - 多级缓存（Caffeine → Redis → Bloom Filter），毫秒级响应
- **访问统计** - Redis Stream 异步消费，多维度统计（PV/UV/地区/设备/浏览器）
- **限流保护** - Guava RateLimiter + Redis Lua 滑动窗口
- **分库分表** - ShardingSphere 16 表水平分片
- **MCP 集成** - 标准 SSE 端点，支持 AI 工具调用

## Tech Stack

| 层级 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.1.10 |
| 构建 | Maven |
| ORM | MyBatis Plus 3.5.5 |
| 数据库 | MySQL 8.0 + ShardingSphere 5.3.2（16 表分片） |
| 缓存 | Caffeine + Redis + Redisson Bloom Filter |
| 消息队列 | Redis Stream + Consumer Group |
| 限流 | Guava RateLimiter + Redis Lua 滑动窗口 |
| IP 定位 | ip2region |
| 前端 | React 19 + TypeScript + Vite 8 + Tailwind CSS 4 |

## 本地启动

### 1. 环境准备

```bash
java -version  # JDK 21
mvn -version   # Maven 3.6+
mysql --version # MySQL 8.0
redis-cli ping  # Redis 7.0 → PONG
```

### 2. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS db_shortlink DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'linkapp'@'127.0.0.1' IDENTIFIED BY 'YourStrongPassword';
GRANT ALL PRIVILEGES ON db_shortlink.* TO 'linkapp'@'127.0.0.1';
FLUSH PRIVILEGES;
```

```bash
mysql -u root -p db_shortlink < link.sql
```

### 3. 构建并启动

```bash
mvn clean package -DskipTests
java -jar target/shortlink-all-1.0-SNAPSHOT.jar
```

或 Docker：

```bash
docker-compose up -d
```

### 4. 前端开发

```bash
cd frontend && npm install && npm run dev
```

### 5. 访问

- **前端应用**：http://127.0.0.1:8068/app
- **API 文档（Scalar）**：http://127.0.0.1:8068/scalar.html
- **Swagger UI**：http://127.0.0.1:8068/swagger-ui/index.html
- **MCP 端点**：http://127.0.0.1:8068/api/mcp

## 核心设计

### 短码生成：号段 + 仿射置换

```
Redis INCRBY → 批量取号段 → 仿射变换 y=(ai+b)mod62^6 → Base62 → 6位短码
```

- 号段模式批量取号，减少 Redis 网络往返
- 异步预取：号段剩余 20% 时触发
- 仿射置换打散连续序号，短码不可预测

### 多级缓存

| 层级 | 组件 | TTL |
|------|------|-----|
| L1 | Caffeine | 10min |
| L2 | Redis | 跟随有效期 |
| L3 | Bloom Filter | 持久化 |
| L4 | 空值缓存 | 30min |

### 异步统计

跳转 → XADD Redis Stream → Consumer Group → Lua(PFADD) → 异步入库

> 详细架构说明见 [doc/intro/核心架构.md](doc/intro/核心架构.md)

## 技术亮点

- 短码生成：号段 + 仿射置换 + Base62
- 多级缓存：Caffeine → Redis → Bloom Filter → 空值缓存
- 异步统计：Redis Stream + Consumer Group + 幂等消费
- 分库分表：ShardingSphere 16 表水平分片
- 限流流控：Guava RateLimiter + Redis Lua 滑动窗口
- MCP 集成：标准 SSE 端点，AI Agent 可调用

> 完整技术亮点见 [doc/intro/技术亮点.md](doc/intro/技术亮点.md)

## API 接口

> 完整接口文档见 [doc/intro/API接口文档.md](doc/intro/API接口文档.md) 或启动后访问 Scalar UI

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{shortUri}` | 302 重定向 |
| POST | `/api/short-link/v1/create` | 创建短链接 |
| POST | `/api/short-link/v1/create/batch` | 批量创建 |
| POST | `/api/short-link/v1/update` | 更新短链接 |
| GET | `/api/short-link/v1/page` | 分页查询 |
| GET | `/api/short-link/v1/stats` | 访问统计 |
| POST | `/api/short-link/admin/v1/user` | 用户注册 |
| POST | `/api/short-link/admin/v1/user/login` | 用户登录 |
| GET | `/api/short-link/admin/v1/group` | 分组列表 |

## 文档目录

| 文档 | 说明 |
|------|------|
| [核心架构](doc/intro/核心架构.md) | 分层架构、核心流程、关键类职责 |
| [技术亮点](doc/intro/技术亮点.md) | 短码生成、多级缓存、异步统计等 |
| [API 接口文档](doc/intro/API接口文档.md) | REST API 详细说明 |
| [开发者准则](doc/rules/开发者准则.md) | 分支管理、文档同步、提交规范等 |
| [未来计划](doc/plan/未来计划.md) | 高级功能、高可用、开放平台等 |
| [开发记录](doc/dev/) | 每次开发的变更记录 |

## Project Structure

```
src/main/java/dev/haotangyuan/shortlink/
├── common/           # 配置、过滤器、异常处理
├── controller/       # 接口层（admin + core）
├── service/          # 业务逻辑层
├── dao/              # 数据访问层（entity + mapper）
├── dto/              # 请求参数对象
├── vo/               # 响应视图对象
├── mq/               # Redis Stream 生产者/消费者
├── mcp/              # MCP AI 工具集成
└── toolkit/          # 工具类（短码生成、IP 地理位置）
```
