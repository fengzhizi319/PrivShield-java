# 部署示例

## 1. 本地开发启动

```bash
# 1. 启动 Java Agent
cd agent/agent-server
mvn spring-boot:run

# 2. 启动 Go Backend
cd console/backend-go
go run cmd/server/main.go

# 3. 启动前端
cd console/web
pnpm dev
```

## 2. Docker Compose 启动

```bash
cd deploy/docker-compose
docker-compose up -d

# 验证
curl http://localhost:8079/v1/health
curl http://localhost:8081/health
curl http://localhost:5174
```

## 3. Kubernetes 部署

```bash
# 应用部署清单
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/agent-deployment.yaml
kubectl apply -f deploy/k8s/agent-service.yaml
kubectl apply -f deploy/k8s/backend-deployment.yaml
kubectl apply -f deploy/k8s/backend-service.yaml
kubectl apply -f deploy/k8s/ingress.yaml

# 验证
kubectl get pods -n privshield
kubectl logs -n privshield -l app=agent
```

## 4. 端到端验证

```bash
# 健康检查
curl http://localhost:8079/v1/health

# 功能验证
curl -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "13812345678", "field_name": "mobile"}'
```
