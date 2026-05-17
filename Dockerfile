# 使用 Eclipse Temurin 17 JRE 基础镜像
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 安装 curl 用于健康检查和时区设置
RUN apt-get update && apt-get install -y curl tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# 复制应用 JAR 文件
COPY target/*.jar app.jar

# 复制默认配置文件（作为 fallback）
COPY src/main/resources/application.yaml application-default.yaml
COPY src/main/resources/shardingsphere-config.yaml shardingsphere-default.yaml

# 复制资源文件到镜像中
COPY src/main/resources/ip2region.xdb ip2region.xdb

# 复制启动脚本
COPY docker-entrypoint.sh /docker-entrypoint.sh

# 更改文件所有者
RUN chown -R appuser:appuser /app && \
    chmod +x /docker-entrypoint.sh

# 切换到应用用户
USER appuser

# 暴露端口
EXPOSE 8068

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8068/ || exit 1

# 使用启动脚本
ENTRYPOINT ["/docker-entrypoint.sh"]