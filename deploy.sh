#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

ACTION=deploy
if [[ "${1:-}" == "restart" ]]; then
  ACTION=restart
  shift
fi
SYNC_URL="${1:-}"

PM=""
JAVA_HOME="${JAVA_HOME:-}"
MAVEN_HOME="${MAVEN_HOME:-}"
USE_SUDO_DOCKER=0
COMPOSE=()
COMPOSE_VERSION=""

fail() {
  echo "环境检查未通过: $*" >&2
  exit 1
}

need_root() {
  if [[ "$(id -u)" -ne 0 ]] && ! command -v sudo >/dev/null 2>&1; then
    fail "安装缺失环境需要 root 权限。请使用 root，或先安装 sudo 后再执行。"
  fi
}

run_root() {
  need_root
  if [[ "$(id -u)" -eq 0 ]]; then
    "$@"
  else
    sudo "$@"
  fi
}

detect_os_tools() {
  if command -v apt-get >/dev/null 2>&1; then
    PM=apt
  elif command -v dnf >/dev/null 2>&1; then
    PM=dnf
  elif command -v yum >/dev/null 2>&1; then
    PM=yum
  fi
}

install_packages() {
  need_root
  echo "正在安装: $*"
  case "$PM" in
    apt)
      run_root apt-get update -y
      run_root env DEBIAN_FRONTEND=noninteractive apt-get install -y "$@"
      ;;
    dnf)
      run_root dnf install -y "$@"
      ;;
    yum)
      run_root yum install -y "$@"
      ;;
    *)
      fail "无法识别包管理器，不能自动安装: $*"
      ;;
  esac
}

ensure_download_tools() {
  if command -v curl >/dev/null 2>&1 && command -v tar >/dev/null 2>&1; then
    return
  fi
  echo "正在安装下载工具..."
  install_packages curl ca-certificates tar gzip
}

download() {
  local url="$1" dest="$2"
  echo "正在下载 $url"
  curl -fL --retry 3 --retry-delay 2 -o "$dest" "$url" || return 1
}

java_major() {
  local bin="$1" ver major
  ver="$("$bin" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
  major="${ver%%.*}"
  if [[ "$major" == "1" ]]; then
    major="${ver#1.}"
    major="${major%%.*}"
  fi
  if [[ "$major" =~ ^[0-9]+$ ]]; then
    echo "$major"
  else
    echo 0
  fi
}

java_is_ok() {
  command -v java >/dev/null 2>&1 || return 1
  [[ "$(java_major java)" -ge 11 ]]
}

refresh_java() {
  local nullglob_state candidate
  nullglob_state="$(shopt -p nullglob)"
  shopt -s nullglob
  local candidates=(/usr/local/jdk-11/bin/java /usr/lib/jvm/java-11*/bin/java /usr/lib/jvm/java-17*/bin/java /usr/lib/jvm/java-21*/bin/java)
  for candidate in "${candidates[@]}"; do
    [[ -x "$candidate" ]] || continue
    if [[ "$(java_major "$candidate")" -ge 11 ]]; then
      JAVA_HOME="$(cd "$(dirname "$candidate")/.." && pwd)"
      export JAVA_HOME
      export PATH="$JAVA_HOME/bin:$PATH"
      hash -r
      eval "$nullglob_state"
      return 0
    fi
  done
  eval "$nullglob_state"
  return 1
}

install_temurin() {
  local arch url tmp
  case "$(uname -m)" in
    x86_64|amd64) arch=x64 ;;
    aarch64|arm64) arch=aarch64 ;;
    *) fail "不支持的 CPU 架构: $(uname -m)" ;;
  esac
  ensure_download_tools
  url="https://api.adoptium.net/v3/binary/latest/11/ga/linux/${arch}/jdk/hotspot/normal/eclipse"
  tmp="$(mktemp /tmp/jdk11.XXXXXX.tar.gz)"
  download "$url" "$tmp"
  need_root
  run_root rm -rf /usr/local/jdk-11
  run_root mkdir -p /usr/local/jdk-11
  run_root tar -xzf "$tmp" -C /usr/local/jdk-11 --strip-components=1
  rm -f "$tmp"
}

ensure_java() {
  if java_is_ok; then
    return
  fi
  echo "缺少 JDK 11，开始安装..."
  if [[ "$(uname -s)" != Linux ]]; then
    fail "自动安装 JDK 仅支持 Linux 服务器。"
  fi
  if [[ -n "$PM" ]]; then
    if [[ "$PM" == apt ]]; then
      install_packages openjdk-11-jdk || true
    else
      install_packages java-11-openjdk-devel || true
    fi
    refresh_java || true
  fi
  if java_is_ok; then
    return
  fi
  echo "软件源中没有可用的 JDK 11，改为下载 Temurin 11..."
  install_temurin
  refresh_java || true
  java_is_ok || fail "JDK 11 安装后仍不可用。"
}

ensure_maven() {
  if command -v mvn >/dev/null 2>&1; then
    return
  fi
  echo "缺少 Maven，开始安装..."
  if [[ "$(uname -s)" != Linux ]]; then
    fail "自动安装 Maven 仅支持 Linux 服务器。"
  fi
  if [[ -n "$PM" ]]; then
    install_packages maven || true
    hash -r
  fi
  if command -v mvn >/dev/null 2>&1; then
    return
  fi
  echo "软件源中没有 Maven，改为下载 Maven 3.9.9..."
  ensure_download_tools
  local tmp url
  tmp="$(mktemp /tmp/maven.XXXXXX.tar.gz)"
  if ! download "https://mirrors.aliyun.com/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz" "$tmp"; then
    download "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz" "$tmp"
  fi
  need_root
  run_root rm -rf /usr/local/maven
  run_root mkdir -p /usr/local/maven
  run_root tar -xzf "$tmp" -C /usr/local/maven --strip-components=1
  rm -f "$tmp"
  run_root ln -sfn /usr/local/maven/bin/mvn /usr/local/bin/mvn
  export MAVEN_HOME=/usr/local/maven
  export PATH="/usr/local/maven/bin:/usr/local/bin:$PATH"
  hash -r
  command -v mvn >/dev/null 2>&1 || fail "Maven 安装后仍不可用。"
}

ensure_git() {
  if command -v git >/dev/null 2>&1; then
    return
  fi
  echo "缺少 git，开始安装..."
  if [[ "$(uname -s)" != Linux ]]; then
    fail "自动安装 git 仅支持 Linux 服务器。"
  fi
  install_packages git
  hash -r
  command -v git >/dev/null 2>&1 || fail "git 安装后仍不可用。"
}

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "缺少 Docker，开始安装..."
    if [[ "$(uname -s)" != Linux ]]; then
      fail "自动安装 Docker 仅支持 Linux 服务器。"
    fi
    ensure_download_tools
    local script
    script="$(mktemp /tmp/get-docker.XXXXXX.sh)"
    download "https://get.docker.com" "$script"
    need_root
    if ! run_root sh "$script" --mirror Aliyun; then
      echo "阿里云镜像安装失败，改用 Docker 官方脚本..."
      run_root sh "$script"
    fi
    rm -f "$script"
    hash -r
  fi
  command -v docker >/dev/null 2>&1 || fail "Docker 安装后仍不可用。"

  if ! docker info >/dev/null 2>&1; then
    echo "正在启动 Docker..."
    need_root
    if command -v systemctl >/dev/null 2>&1; then
      run_root systemctl enable --now docker || run_root systemctl start docker
    else
      run_root service docker start
    fi
  fi

  if docker info >/dev/null 2>&1; then
    USE_SUDO_DOCKER=0
    return
  fi

  if [[ "$(id -u)" -ne 0 ]]; then
    run_root usermod -aG docker "$USER" || true
  fi
  if run_root docker info >/dev/null 2>&1; then
    USE_SUDO_DOCKER=1
    echo "当前用户尚未获得 Docker 组权限，本次使用 sudo 运行 Docker。重新登录后可直接使用 docker。"
    return
  fi
  fail "Docker 已安装，但服务无法启动。"
}

docker_cmd() {
  if [[ "$USE_SUDO_DOCKER" == 1 ]]; then
    sudo docker "$@"
  else
    docker "$@"
  fi
}

set_compose_command() {
  if docker_cmd compose version >/dev/null 2>&1; then
    if [[ "$USE_SUDO_DOCKER" == 1 ]]; then
      COMPOSE=(sudo docker compose)
    else
      COMPOSE=(docker compose)
    fi
    COMPOSE_VERSION="$(docker_cmd compose version 2>/dev/null || true)"
    return 0
  fi
  if [[ "$USE_SUDO_DOCKER" == 0 ]] && command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
    COMPOSE_VERSION="$(docker-compose version 2>/dev/null || true)"
    return 0
  fi
  if [[ "$USE_SUDO_DOCKER" == 1 ]] && sudo docker-compose version >/dev/null 2>&1; then
    COMPOSE=(sudo docker-compose)
    COMPOSE_VERSION="$(sudo docker-compose version 2>/dev/null || true)"
    return 0
  fi
  return 1
}

ensure_compose() {
  if set_compose_command; then
    return
  fi

  echo "缺少 Docker Compose，开始安装..."
  if [[ -n "$PM" ]]; then
    install_packages docker-compose-plugin || install_packages docker-compose || true
    hash -r
  fi
  if set_compose_command; then
    return
  fi

  echo "改为下载 Docker Compose 二进制..."
  ensure_download_tools
  local arch dest tmp
  case "$(uname -m)" in
    x86_64|amd64) arch=x86_64 ;;
    aarch64|arm64) arch=aarch64 ;;
    *) fail "不支持的 CPU 架构: $(uname -m)" ;;
  esac
  dest="/usr/local/lib/docker/cli-plugins/docker-compose"
  tmp="$(mktemp /tmp/docker-compose.XXXXXX)"
  if ! download "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${arch}" "$tmp"; then
    download "https://mirrors.aliyun.com/docker-toolbox/linux/compose/v2.29.7/docker-compose-linux-${arch}" "$tmp"
  fi
  need_root
  run_root mkdir -p /usr/local/lib/docker/cli-plugins
  run_root mv "$tmp" "$dest"
  run_root chmod +x "$dest"
  run_root ln -sfn "$dest" /usr/local/bin/docker-compose
  hash -r
  set_compose_command || fail "Docker Compose 安装后仍不可用。"
}

prepare_config() {
  mkdir -p config data logs
  if [[ ! -f config/application.yml ]]; then
    cp config/application.yml.example config/application.yml
    echo "已生成 config/application.yml"
  fi
  if [[ -n "$SYNC_URL" ]]; then
    SYNC_URL="${SYNC_URL%/}"
    local tmp
    tmp="$(mktemp)"
    awk -v url="$SYNC_URL" '
      /^[[:space:]]*base-url:/ { print "    base-url: " url; next }
      { print }
    ' config/application.yml > "$tmp"
    mv "$tmp" config/application.yml
  fi
  if ! grep -Eq '^[[:space:]]*base-url:[[:space:]]*http' config/application.yml; then
    echo "请在 config/application.yml 中填写 sync.api.base-url，或执行: ./deploy.sh http://地址:端口" >&2
    exit 1
  fi
  ensure_h2_url
}

ensure_h2_url() {
  local file="config/application.yml"
  local url="jdbc:h2:file:./data/newsdb;DB_CLOSE_ON_EXIT=FALSE"
  local tmp
  if grep -q 'AUTO_SERVER=TRUE' "$file"; then
    tmp="$(mktemp)"
    sed "s|jdbc:h2:[^[:space:]]*|${url}|g" "$file" > "$tmp"
    mv "$tmp" "$file"
    echo "已关闭 H2 自动服务模式，避免主机名无法解析。"
    return
  fi
  if grep -q 'jdbc:h2:' "$file"; then
    return
  fi
  cat >> "$file" <<EOF

spring:
  datasource:
    url: ${url}
EOF
  echo "已写入 H2 数据库地址，避免主机名无法解析。"
}

echo "正在检查基础环境..."
detect_os_tools
if [[ "$ACTION" == "restart" ]]; then
  ensure_docker
  ensure_compose
else
  ensure_git
  ensure_java
  ensure_maven
  ensure_docker
  ensure_compose
fi

echo "基础环境检查通过"
echo "  docker  $(docker --version)"
echo "  compose ${COMPOSE_VERSION}"
if [[ "$ACTION" != "restart" ]]; then
  JAVA_VERSION="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
  echo "  git     $(git --version)"
  echo "  java    ${JAVA_VERSION}"
  echo "  maven   $(mvn -version 2>/dev/null | awk 'NR==1 {print $3}')"
fi

if [[ "$ACTION" != "restart" ]] && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git pull --ff-only
fi

prepare_config

if [[ "$ACTION" == "restart" ]]; then
  echo "按 config/application.yml 重新启动，不重新打包..."
  "${COMPOSE[@]}" up -d --force-recreate --no-build
  echo "已重启。当前配置:"
  grep -E 'base-url:|fixed-delay:|days:' config/application.yml
  echo "查看日志: ${COMPOSE[*]} logs -f"
  exit 0
fi

mkdir -p data logs

MAVEN_SETTINGS="$(mktemp /tmp/wiseflow-maven.XXXXXX.xml)"
cat > "$MAVEN_SETTINGS" <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

echo "正在使用源码打包..."
mvn -B -DskipTests -s "$MAVEN_SETTINGS" clean package
rm -f "$MAVEN_SETTINGS"

JAR="target/wiseflow_java-1.0-SNAPSHOT.jar"
if [[ ! -f "$JAR" ]]; then
  echo "打包失败，未生成 ${JAR}" >&2
  exit 1
fi
cp "$JAR" app.jar

echo "源码打包完成，开始构建镜像并启动..."
"${COMPOSE[@]}" up -d --build

echo "部署完成。运行配置: config/application.yml"
grep -E 'base-url:|fixed-delay:|days:' config/application.yml
echo "以后修改该文件后执行: ./deploy.sh restart"
echo "查看状态: ${COMPOSE[*]} ps"
echo "查看日志: ${COMPOSE[*]} logs -f"
