#!/usr/bin/env bash
# =============================================================================
# dev-start-agent.sh — 启动 Java Agent (REST :8079 + gRPC :50051)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== PrivShield Java Agent ==="
echo "Agent dir: $AGENT_DIR"

# 编译
echo "[1/2] Building agent-server..."
cd "$AGENT_DIR"
mvn clean package -DskipTests -q

# 启动
echo "[2/2] Starting Java Agent (REST :8079 + gRPC :50051)..."
java -jar agent-server/target/agent-server.jar \
  --server.port="${PRIVACY_REST_PORT:-8079}" \
  --grpc.server.port="${PRIVACY_GRPC_PORT:-50051}"
