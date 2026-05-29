#!/bin/bash

set -e

echo "🚀 Starting ShortLink Application..."

# 检查配置文件是否存在
if [ -f "/app/application.yaml" ]; then
    echo "✅ Using application.yaml"
else
    echo "📋 Using default application.yaml"
    cp /app/application-default.yaml /app/application.yaml
fi

if [ -f "/app/shardingsphere-config.yaml" ]; then
    echo "✅ Using shardingsphere-config.yaml"
else
    echo "📋 Using default shardingsphere-config.yaml"
    cp /app/shardingsphere-default.yaml /app/shardingsphere-config.yaml
fi

# 检查必要的资源文件
if [ ! -f "/app/ip2region.xdb" ]; then
    echo "❌ ip2region.xdb not found!"
    exit 1
fi

# 显示启动信息
echo "📁 Configuration files:"
echo "   - Application config: $([ -f '/app/application.yaml' ] && echo '✅ Found' || echo '❌ Missing')"
echo "   - ShardingSphere config: $([ -f '/app/shardingsphere-config.yaml' ] && echo '✅ Found' || echo '❌ Missing')"
echo "   - IP region database: $([ -f '/app/ip2region.xdb' ] && echo '✅ Found' || echo '❌ Missing')"

# 启动应用
echo "🎯 Starting application with Spring profile: docker"
exec java ${JAVA_OPTS:--Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200} \
    -jar \
    -Dspring.config.location=file:/app/application.yaml \
    -Dspring.profiles.active=docker \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    app.jar