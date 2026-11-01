#!/usr/bin/env bash
# 在新服务器上重新部署。用法：
#   bash redeploy.sh http://同步服务地址:端口
set -euo pipefail

REPO_URL="https://github.com/kentPhilippines/wiseflow_java.git"
BRANCH="php_main"
INSTALL_DIR="${INSTALL_DIR:-/opt/wiseflow_java}"
SYNC_URL="${1:-}"

if [[ -z "$SYNC_URL" ]]; then
  echo "用法: bash redeploy.sh http://同步服务地址:端口"
  echo "示例: bash redeploy.sh http://192.168.1.10:18082"
  exit 1
fi

SYNC_URL="${SYNC_URL%/}"

run_root() {
  if [[ "$(id -u)" -eq 0 ]]; then
    "$@"
  elif command -v sudo >/dev/null 2>&1; then
    sudo "$@"
  else
    echo "需要 root 权限。请使用 root，或先安装 sudo。" >&2
    exit 1
  fi
}

install_git() {
  if command -v git >/dev/null 2>&1; then
    return
  fi
  echo "正在安装 git..."
  if command -v apt-get >/dev/null 2>&1; then
    run_root apt-get update -y
    run_root env DEBIAN_FRONTEND=noninteractive apt-get install -y git ca-certificates
  elif command -v dnf >/dev/null 2>&1; then
    run_root dnf install -y git ca-certificates
  elif command -v yum >/dev/null 2>&1; then
    run_root yum install -y git ca-certificates
  else
    echo "无法识别包管理器，请先手动安装 git。" >&2
    exit 1
  fi
}

echo "开始在新服务器部署，目录: ${INSTALL_DIR}"
echo "同步地址: ${SYNC_URL}"

install_git

if [[ -d "${INSTALL_DIR}/.git" ]]; then
  echo "目录已存在，拉取最新代码..."
  git -C "$INSTALL_DIR" fetch origin "$BRANCH"
  git -C "$INSTALL_DIR" checkout "$BRANCH"
  git -C "$INSTALL_DIR" pull --ff-only origin "$BRANCH"
else
  run_root mkdir -p "$(dirname "$INSTALL_DIR")"
  run_root git clone --branch "$BRANCH" "$REPO_URL" "$INSTALL_DIR"
  if [[ "$(id -u)" -ne 0 ]]; then
    run_root chown -R "$(id -u):$(id -g)" "$INSTALL_DIR"
  fi
fi

cd "$INSTALL_DIR"
bash ./deploy.sh "$SYNC_URL"
