# =============================================================================
# Dockerfile — PrivShield Java Agent
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY agent-sdk/pom.xml agent-sdk/
COPY agent-server/pom.xml agent-server/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源码并编译
COPY agent-sdk/src agent-sdk/src
COPY agent-server/src agent-server/src

RUN mvn clean package -DskipTests -B

# ─── Runtime ───
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从 builder 复制 fat JAR
COPY --from=builder /build/agent-server/target/agent-server.jar app.jar

# 复制配置和规则
COPY agent/config/ /app/config/
COPY agent/rules/ /app/rules/
COPY agent/data/ /app/data/

# 环境变量
ENV PRIVACY_REST_PORT=8079
ENV PRIVACY_GRPC_PORT=50051
ENV JAVA_OPTS="-Xms512m -Xmx1g"

# 暴露端口
EXPOSE 8079 50051

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:8079/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=$PRIVACY_REST_PORT --grpc.server.port=$PRIVACY_GRPC_PORT"]
