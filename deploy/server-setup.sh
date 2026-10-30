#!/usr/bin/env bash
# =============================================================================
# 凡人修仙博客 - 服务器一键初始化
# 适用系统：Alibaba Cloud Linux 3 / Anolis 8 / CentOS 8 Stream（dnf 系）
# 用法（root 执行）：
#     bash server-setup.sh [公网IP]
# 例：
#     bash server-setup.sh 118.31.186.244
#
# 做四件事：
#   1. 安装 JDK 17 + MySQL 8，MySQL 按 2G 内存做轻量调优
#   2. 建库 blog、建库账号 blog，初始化应用运行目录 /opt/spring-blog
#   3. 生成 app.env（随机数据库密码 + 随机 JWT 密钥，权限 600）
#   4. 注册 systemd 服务 spring-blog（开机自启）
#
# 脚本可重复执行：已存在的库、用户、env、服务都不会被覆盖。
# =============================================================================
set -euo pipefail

APP_NAME="spring-blog"
APP_DIR="/opt/${APP_NAME}"
APP_USER="springblog"
JAR="${APP_DIR}/app.jar"
ENV_FILE="${APP_DIR}/app.env"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"
DB_NAME="blog"
DB_USER="blog"
APP_PORT="8081"

log() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m[失败] %s\033[0m\n' "$*" >&2; exit 1; }

[[ ${EUID} -eq 0 ]] || die "请用 root 执行（sudo -i 后再跑）"

# ---------- 0. 公网 IP ----------
PUBLIC_IP="${1:-}"
if [[ -z "${PUBLIC_IP}" ]]; then
    PUBLIC_IP="$(curl -s --max-time 3 http://100.100.100.100/latest/meta-data/eipv4 || true)"
fi
[[ -n "${PUBLIC_IP}" ]] || PUBLIC_IP="$(hostname -I | awk '{print $1}')"
[[ -n "${PUBLIC_IP}" ]] || die "无法确定公网 IP，请手动传入：bash server-setup.sh <公网IP>"

# ---------- 1. JDK 17 ----------
log "1/7 检查并安装 JDK 17"
# 机器上可能已存在 JDK 24（/usr/local/jdk-24.x），Spring Boot 3.4 在 24 上不保证兼容，
# 因此这里始终确保 JDK 17 存在，并显式用它启动，不依赖 PATH 与 alternatives。
JAVA_BIN=""
for _cand in /usr/lib/jvm/java-17-openjdk*/bin/java /usr/lib/jvm/jre-17/bin/java; do
    if [[ -x "${_cand}" ]]; then JAVA_BIN="${_cand}"; break; fi
done
if [[ -z "${JAVA_BIN}" ]]; then
    dnf install -y java-17-openjdk-headless
    for _cand in /usr/lib/jvm/java-17-openjdk*/bin/java /usr/lib/jvm/jre-17/bin/java; do
        if [[ -x "${_cand}" ]]; then JAVA_BIN="${_cand}"; break; fi
    done
fi
if [[ -z "${JAVA_BIN}" ]]; then
    JAVA_BIN="$(readlink -f "$(command -v java)")"
    echo "[警告] 未找到 JDK 17，回退使用 ${JAVA_BIN}"
fi
[[ -x "${JAVA_BIN}" ]] || die "找不到可用的 java，请手动安装 JDK 17"
echo "Java 路径: ${JAVA_BIN}"
echo "版本    : $(${JAVA_BIN} -version 2>&1 | head -n1)"

# ---------- 2. MySQL 8 ----------
log "2/7 检查并安装 MySQL 8"
if ! command -v mysqld >/dev/null 2>&1; then
    dnf install -y mysql-server
fi
systemctl enable --now mysqld
# 等 MySQL 起来（首次初始化较慢）；用 root 直连探测，兼容空密码与 auth_socket 两种初始化方式
for _ in $(seq 1 30); do
    mysql -uroot -e 'SELECT 1' >/dev/null 2>&1 && break
    sleep 2
done
mysql -uroot -e 'SELECT 1' >/dev/null 2>&1 || die "MySQL 无法用 root 登录，请查看 journalctl -u mysqld -n 50"

# ---------- 3. MySQL 轻量调优（2G 内存） ----------
log "3/7 MySQL 调优（2G 内存场景）"
SMALL_CNF="/etc/my.cnf.d/zz-blog-small.cnf"
if [[ ! -f "${SMALL_CNF}" ]]; then
    cat > "${SMALL_CNF}" <<'CNF'
# 2G 内存 ECS 专用：压住 MySQL 常驻内存，避免与应用抢内存被 OOM
[mysqld]
innodb_buffer_pool_size = 128M
max_connections = 100
performance_schema = OFF
table_open_cache = 400
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
CNF
    systemctl restart mysqld
    for _ in $(seq 1 30); do
        mysql -uroot -e 'SELECT 1' >/dev/null 2>&1 && break
        sleep 2
    done
    echo "已写入 ${SMALL_CNF}"
else
    echo "已存在 ${SMALL_CNF}，跳过"
fi

# ---------- 4. 运行目录与系统用户 ----------
log "4/7 创建应用目录与运行用户"
id -u "${APP_USER}" >/dev/null 2>&1 || \
    useradd -r -s /sbin/nologin -d "${APP_DIR}" "${APP_USER}"
mkdir -p "${APP_DIR}/logs" "${APP_DIR}/data/uploads"

# ---------- 5. 数据库与账号 ----------
log "5/7 初始化数据库 ${DB_NAME}"
if [[ -f "${ENV_FILE}" ]]; then
    DB_PASS="$(grep -E '^DB_PASSWORD=' "${ENV_FILE}" | cut -d= -f2-)"
    echo "复用 app.env 中已有的数据库密码"
else
    DB_PASS="$(openssl rand -base64 32 | tr -d '/+=' | cut -c1-24)"
fi
[[ -n "${DB_PASS}" ]] || die "数据库密码为空，请检查 ${ENV_FILE}"

mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL
echo "库 ${DB_NAME} 与账号 ${DB_USER}@localhost 就绪"

# ---------- 6. 应用环境变量 ----------
log "6/7 生成 ${ENV_FILE}"
if [[ -f "${ENV_FILE}" ]]; then
    echo "已存在，保留原配置（如需改端口/密钥请手工编辑）"
else
    JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n/+=' | cut -c1-56)"
    cat > "${ENV_FILE}" <<EOF
# ===== 由 server-setup.sh 生成，请勿提交到 git =====
TZ=Asia/Shanghai

# 应用
SERVER_PORT=${APP_PORT}
FILE_UPLOAD_DIR=${APP_DIR}/data/uploads
CORS_ORIGINS=http://${PUBLIC_IP}:${APP_PORT},http://127.0.0.1:${APP_PORT}

# 数据库
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASS}

# JWT（务必保密，泄露等于任何人可伪造登录态）
JWT_SECRET=${JWT_SECRET}

# AI 助手：填 DeepSeek/通义等 OpenAI 兼容服务的 Key；不填则 AI 功能不可用
AI_API_KEY=not-set

# 日志
LOG_HOME=${APP_DIR}/logs
LOG_LEVEL_APP=INFO
EOF
    chmod 600 "${ENV_FILE}"
    echo "已生成随机数据库密码与 JWT 密钥"
fi

# ---------- 7. systemd 服务 ----------
log "7/7 注册 systemd 服务"
cat > "${SERVICE_FILE}" <<EOF
[Unit]
Description=Spring Blog (凡人修仙博客)
Documentation=https://github.com/StatedFast/Martial-Arts-Ascension-Blog
After=network-online.target mysqld.service
Wants=network-online.target mysqld.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}

# 2G 内存：堆上限 512M，元空间封顶，避免撑爆被 OOM Killer 干掉
ExecStart=${JAVA_BIN} \\
    -Xms256m -Xmx512m \\
    -XX:MaxMetaspaceSize=256m \\
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${APP_DIR}/logs \\
    -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \\
    -jar ${JAR}

SuccessExitStatus=143
Restart=on-failure
RestartSec=10
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"
systemctl daemon-reload
systemctl enable "${APP_NAME}" >/dev/null 2>&1 || true

# 应用自身日志走 logback 文件，这里只补一条可读的启动摘要
if systemctl is-active --quiet firewalld; then
    firewall-cmd --permanent --add-port="${APP_PORT}/tcp" >/dev/null && firewall-cmd --reload >/dev/null
    echo "firewalld 已放行 ${APP_PORT}/tcp"
fi

if [[ -f "${JAR}" ]]; then
    systemctl restart "${APP_NAME}"
    echo "已启动 ${APP_NAME}"
else
    echo "app.jar 尚未上传，服务已注册但未启动"
fi

# ---------- 收尾检查 ----------
log "环境检查"
echo "Java   : $(java -version 2>&1 | head -n1)"
echo "MySQL  : $(mysql --version)"
echo "内存   : $(free -h | awk '/Mem:/{print $2" 总 / "$7" 可用"}')"
echo "Swap   : $(free -h | awk '/Swap:/{print $2}')"
echo "公网IP : ${PUBLIC_IP}"
echo "应用目录: ${APP_DIR}"

cat <<EOF

------------------------------------------------------------
后续三步：
  1) 本机打包后上传 jar：
       mvnw.cmd -DskipTests package
       scp target/Martial-Arts-Ascension-Blog-0.0.1-SNAPSHOT.jar root@${PUBLIC_IP}:${JAR}

  2) 服务器启动：
       systemctl restart ${APP_NAME}
       systemctl status ${APP_NAME} --no-pager

  3) 阿里云控制台 -> 安全组 -> 入方向放行 ${APP_PORT}/tcp
     然后访问： http://${PUBLIC_IP}:${APP_PORT}

常用运维：
   journalctl -u ${APP_NAME} -f          实时看控制台输出
   tail -f ${APP_DIR}/logs/app/*.log     业务日志
   tail -f ${APP_DIR}/logs/operation/*.log  操作日志
------------------------------------------------------------
EOF
