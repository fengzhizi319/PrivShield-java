# 双协议网关负载均衡

> 支持 gRPC + REST 双协议的负载均衡网关，提供智能路由、健康检查和故障转移能力。

## 架构

```
Client → Gateway(:80/:443)
           ├── gRPC Proxy → Agent(:50051) [多实例]
           └── REST Proxy → Agent(:8079) [多实例]
```

## 核心能力

- **双协议支持**: gRPC 和 REST 统一入口
- **智能路由**: 基于权重的负载均衡
- **健康检查**: 主动/被动健康检查
- **故障转移**: 自动摘除不健康节点
- **连接池**: HTTP/2 多路复用

## 快速开始

```bash
# 启动网关
java -jar gateway.jar --config=gateway.yaml

# 验证
curl http://gateway:80/health
```
