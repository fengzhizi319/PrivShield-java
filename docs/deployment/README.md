# 部署指南

> PrivShield Java 支持本地开发、Docker、K8s 三种部署模式。

## 本地开发

```bash
# 1. Java Agent
cd agent && mvn clean package -DskipTests
java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT.jar

# 2. Go Backend
cd console/backend-go && go build -o server ./cmd/server && ./server

# 3. Frontend
cd console/web && npm install && npm run dev
```

## Docker 部署

```bash
cd agent
docker build -t privshield-java-agent:latest .
docker run -p 8079:8079 -p 50051:50051 privshield-java-agent:latest
```

## 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 5174 | Vite 开发服务器 |
| Go Backend | 8081 | HTTP API 代理 |
| Java Agent REST | 8079 | REST API |
| Java Agent gRPC | 50051 | gRPC API |
