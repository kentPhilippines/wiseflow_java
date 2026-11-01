#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

fail() {
  echo "环境检查未通过: $*" >&2
  exit 1
}

echo "正在检查基础环境..."

if ! command -v git >/dev/null 2>&1; then
  fail "未安装 git。请先安装后再执行部署。"
fi

if ! command -v docker >/dev/null 2>&1; then
  fail "未安装 Docker。请先安装 Docker Engine。"
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
  COMPOSE_VERSION="$(docker compose version 2>/dev/null || true)"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
  COMPOSE_VERSION="$(docker-compose version 2>/dev/null || true)"
else
  fail "未安装 Docker Compose。请安装 docker compose 插件。"
fi

set +e
DOCKER_INFO_ERR="$(docker info 2>&1 >/dev/null)"
DOCKER_INFO_CODE=$?
set -e
if [[ "$DOCKER_INFO_CODE" -ne 0 ]]; then
  if printf '%s' "$DOCKER_INFO_ERR" | grep -qi 'permission denied'; then
    fail "当前用户无权访问 Docker。请使用 root，或把用户加入 docker 组后重新登录。"
  fi
  fail "Docker 服务未运行。请先执行: systemctl start docker"
fi

echo "基础环境检查通过"
echo "  git     $(git --version)"
echo "  docker  $(docker --version)"
echo "  compose ${COMPOSE_VERSION}"

if [[ $# -ge 1 ]]; then
  SYNC_API_BASE_URL="$1"
fi

if [[ -z "${SYNC_API_BASE_URL:-}" ]]; then
  echo "请指定同步接口根地址，不要带末尾路径。"
  echo "用法: ./deploy.sh http://192.168.1.10:18082"
  exit 1
fi

SYNC_API_BASE_URL="${SYNC_API_BASE_URL%/}"

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git pull --ff-only
fi

printf 'SYNC_API_BASE_URL=%s\n' "$SYNC_API_BASE_URL" > .env
mkdir -p data logs

echo "开始在服务器上编译打包并启动..."
"${COMPOSE[@]}" up -d --build

echo "部署完成。同步地址: ${SYNC_API_BASE_URL}"
echo "查看状态: ${COMPOSE[*]} ps"
echo "查看日志: ${COMPOSE[*]} logs -f"
