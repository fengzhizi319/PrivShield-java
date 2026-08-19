#!/usr/bin/env bash
# =============================================================================
# dev-start-all.sh — 一键启动三端 (Java Agent + Go 后端 + Vite 前端)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
AGENT_DIR="$ROOT_DIR/agent"
BACKEND_DIR="$ROOT_DIR/console/backend-go"
FRONTEND_DIR="$ROOT_DIR/console/web"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log() { echo -e "${CYAN}[start-all]${NC} $*"; }

cleanup() {
    log "Shutting down all services..."
    [ -n "${AGENT_PID:-}" ] && kill "$AGENT_PID" 2>/dev/null && log "Agent stopped"
    [ -n "${BACKEND_PID:-}" ] && kill "$BACKEND_PID" 2>/dev/null && log "Go backend stopped"
    [ -n "${FRONTEND_PID:-}" ] && kill "$FRONTEND_PID" 2>/dev/null && log "Frontend stopped"
    exit 0
}
trap cleanup SIGINT SIGTERM

# ─── 1. 启动 Java Agent ───
log "Building and starting Java Agent..."
cd "$AGENT_DIR"
mvn clean package -DskipTests -q
java -jar agent-server/target/agent-server.jar \
  --server.port="${PRIVACY_REST_PORT:-8079}" \
  --grpc.server.port="${PRIVACY_GRPC_PORT:-50051}" &
AGENT_PID=$!
log "Agent started (PID=$AGENT_PID, REST=:${PRIVACY_REST_PORT:-8079}, gRPC=:${PRIVACY_GRPC_PORT:-50051})"

# ─── 2. 启动 Go 后端 ───
log "Starting Go backend..."
cd "$BACKEND_DIR"
go run ./cmd/server/ &
BACKEND_PID=$!
log "Go backend started (PID=$BACKEND_PID, :${BACKEND_PORT:-8081})"

# ─── 3. 启动 Vite 前端 ───
log "Starting frontend..."
cd "$FRONTEND_DIR"
npm run dev &
FRONTEND_PID=$!
log "Frontend started (PID=$FRONTEND_PID, :5173)"

# ─── 4. 等待就绪 ───
log "Waiting for services to be ready..."
for i in $(seq 1 30); do
    if curl -sf http://localhost:${PRIVACY_REST_PORT:-8079}/health > /dev/null 2>&1; then
        log "Java Agent is ready!"
        break
    fi
    sleep 1
done

echo ""
echo -e "${GREEN}=== All services started ===${NC}"
echo -e "  Frontend:     ${YELLOW}http://localhost:5173${NC}"
echo -e "  Go Backend:   ${YELLOW}http://localhost:${BACKEND_PORT:-8081}${NC}"
echo -e "  Java Agent:   ${YELLOW}REST=:${PRIVACY_REST_PORT:-8079}  gRPC=:${PRIVACY_GRPC_PORT:-50051}${NC}"
echo ""

# ─── 5. Watchdog ───
wait
