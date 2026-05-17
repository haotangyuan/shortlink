# 部署指南

使用预构建镜像 `ghcr.io/haotangyuan/shortlink` 部署，无需本地编译。

## 快速开始

```bash
# 下载部署文件
mkdir shortlink && cd shortlink
BASE_URL="https://raw.githubusercontent.com/haotangyuan/shortlink/main"
curl -O "$BASE_URL/docker compose.yml"
curl -O "$BASE_URL/.env.example"
curl -O "$BASE_URL/application-docker.yaml"
curl -O "$BASE_URL/shardingsphere-config-docker.yaml"
curl -O "$BASE_URL/link.sql"

# 1. 配置环境变量
cp .env.example .env
nano .env                              # 修改 MySQL/Redis 密码

# 2. 编辑 ShardingSphere 数据库配置
nano shardingsphere-config-docker.yaml # 第 9 行 password 改为与 .env 中 MYSQL_PASSWORD 一致

# 3. （可选）修改应用配置
nano application-docker.yaml           # 域名、限流、白名单等

# 启动
docker compose up -d
```

## 配置说明

### .env（环境变量）

```bash
MYSQL_ROOT_PASSWORD=<your-password>   # MySQL root 密码
MYSQL_PASSWORD=<your-password>        # MySQL 应用用户密码
REDIS_PASSWORD=<your-password>        # Redis 密码
SHORTLINK_DOMAIN=yourdomain.com       # 短链接域名
JAVA_OPTS=-Xmx1024m -Xms512m          # JVM 参数
```

### shardingsphere-config-docker.yaml（分库分表）

```yaml
dataSources:
  ds_0:
    jdbcUrl: jdbc:mysql://shortlink-mysql:3306/db_shortlink?...
    username: linkapp
    password: <与 MYSQL_PASSWORD 一致>   # 第 9 行
```

### application-docker.yaml（应用配置）

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:YourStrongPassword}  # 支持环境变量

short-link:
  domain:
    default: ${SHORTLINK_DOMAIN:localhost:8068}       # 短链接域名
  rate-limit:
    redirect:
      rps: 1000                                       # 跳转限流
  goto-domain:
    white-list:
      enable: true                                    # 域名白名单
      details:
        - github.com
        - zhihu.com
```

## 常用命令

```bash
docker compose ps                           # 查看状态
docker compose logs -f shortlink-app        # 查看日志
docker compose restart shortlink-app        # 重启应用
docker compose pull && docker compose up -d # 更新镜像
docker compose down                         # 停止服务
```

## 反向代理

### Nginx

```bash
curl -o go.conf https://raw.githubusercontent.com/haotangyuan/shortlink/main/shortlink.nginx.conf
nano go.conf
sudo cp go.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx
```

### Caddy

```bash
curl -O https://raw.githubusercontent.com/haotangyuan/shortlink/main/Caddyfile

nano Caddyfile
caddy run
```