#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ $# -ge 1 ]]; then
  SYNC_API_BASE_URL="$1"
fi

if [[ -z "${SYNC_API_BASE_URL:-}" ]]; then
  echo "请指定同步接口根地址，不要带末尾路径。"
  echo "用法: ./deploy.sh http://192.168.1.10:18082"
  exit 1
fi

SYNC_API_BASE_URL="${SYNC_API_BASE_URL%/}"

if ! command -v docker >/dev/null 2>&1; then
  echo "未找到 docker，请先安装 Docker。"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "未找到 docker compose，请安装 Docker Compose 插件。"
  exit 1
fi

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git pull --ff-only
fi

printf 'SYNC_API_BASE_URL=%s\n' "$SYNC_API_BASE_URL" > .env
mkdir -p data logs

"${COMPOSE[@]}" up -d --build

echo "部署完成。同步地址: ${SYNC_API_BASE_URL}"
echo "查看状态: ${COMPOSE[*]} ps"
echo "查看日志: ${COMPOSE[*]} logs -f"
