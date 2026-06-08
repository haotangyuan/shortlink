# API 接口文档

> 完整的交互式 API 文档请访问启动后的 Scalar UI：http://localhost:8068/scalar.html
> Swagger UI：http://localhost:8068/swagger-ui/index.html

## 认证说明

### Admin 接口

`/api/short-link/admin/v1/*` 使用 Session Token 认证：

```
Authorization: Bearer <login-返回的token>
```

### Core 接口

`/api/short-link/v1/*` 使用 API Token 认证（后台管理页面创建）。

### 短链跳转

`GET /{短码}` 无需认证。

## 接口列表

### 用户管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/short-link/admin/v1/user` | 用户注册 | ❌ |
| POST | `/api/short-link/admin/v1/user/login` | 用户登录 | ❌ |
| GET | `/api/short-link/admin/v1/user` | 获取用户信息 | ✅ |
| PUT | `/api/short-link/admin/v1/user` | 更新用户信息 | ✅ |

### 分组管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/short-link/admin/v1/group` | 分组列表 | ✅ |
| POST | `/api/short-link/admin/v1/group` | 创建分组 | ✅ |
| PUT | `/api/short-link/admin/v1/group` | 更新分组 | ✅ |
| DELETE | `/api/short-link/admin/v1/group` | 删除分组 | ✅ |

### 短链接管理（Admin）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/short-link/admin/v1/create` | 创建短链接 | ✅ |
| POST | `/api/short-link/admin/v1/create/batch` | 批量创建 | ✅ |
| POST | `/api/short-link/admin/v1/update` | 更新短链接 | ✅ |
| GET | `/api/short-link/admin/v1/page` | 分页查询 | ✅ |
| GET | `/api/short-link/admin/v1/count` | 分组短链接数量 | ✅ |

### 短链接管理（Core）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/short-link/v1/create` | 创建短链接 | API Token |
| POST | `/api/short-link/v1/create/batch` | 批量创建 | API Token |
| POST | `/api/short-link/v1/update` | 更新短链接 | API Token |
| GET | `/api/short-link/v1/page` | 分页查询 | API Token |
| GET | `/api/short-link/v1/stats` | 访问统计 | API Token |
| GET | `/api/short-link/v1/count` | 分组短链接数量 | API Token |

### 短链跳转

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/{shortUri}` | 302 重定向到原始 URL | ❌ |

### 统计

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/short-link/admin/v1/stats` | 访问统计（Admin） | ✅ |
| GET | `/api/short-link/v1/stats` | 访问统计（Core） | API Token |

### 回收站

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/short-link/admin/v1/recycle-bin/page` | 回收站分页查询 | ✅ |
| POST | `/api/short-link/admin/v1/recycle-bin/restore` | 恢复短链接 | ✅ |

### Token 管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/short-link/admin/v1/token` | Token 列表 | ✅ |
| POST | `/api/short-link/admin/v1/token` | 创建 Token | ✅ |
| DELETE | `/api/short-link/admin/v1/token` | 删除 Token | ✅ |

### MCP

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/mcp` | MCP SSE 端点 | ❌ |

## 核心流程

### 创建短链接

```
1. POST /api/short-link/admin/v1/user/register  → 注册用户
2. POST /api/short-link/admin/v1/user/login      → 获取 Token
3. GET  /api/short-link/admin/v1/group           → 获取分组 gid
4. POST /api/short-link/admin/v1/create          → 创建短链接
5. GET  /{短码}                                   → 验证 302 跳转
```

### 访问统计

```
1. GET /api/short-link/admin/v1/stats?fullShortUrl=xxx  → 查看统计数据
   返回：PV、UV、UIP、地区分布、设备分布、浏览器分布
```

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

错误响应：
```json
{
  "code": 1001,
  "message": "短链接不存在",
  "data": null
}
```
