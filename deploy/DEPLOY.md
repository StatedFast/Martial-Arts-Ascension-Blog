# 部署说明（阿里云 ECS）

## 服务器

| 项 | 值 |
| --- | --- |
| 公网 IP | 118.31.186.244 |
| 系统 | Alibaba Cloud Linux 3 (Anolis 8) |
| 规格 | 2 vCPU / 1.8 GiB / 40 GiB ESSD / 4 GiB swap |
| 运行用户 | `springblog`（nologin 系统用户） |
| 应用目录 | `/opt/spring-blog` |
| 监听端口 | `8081` |
| 域名 | 未配置，使用 `http://118.31.186.244:8081` |

## 服务端组成

| 组件 | 版本 / 位置 |
| --- | --- |
| JDK | OpenJDK 17（`/usr/lib/jvm/java-17-openjdk-*`，显式指定，不受机器上 JDK 24 影响） |
| MySQL | 8.0.46，库 `blog`，账号 `blog@localhost`，密码随机生成在 `app.env` |
| MySQL 调优 | `/etc/my.cnf.d/zz-blog-small.cnf`（buffer pool 128M、关闭 performance_schema，适配 2G 内存） |
| 应用 jar | `/opt/spring-blog/app.jar` |
| 环境变量 | `/opt/spring-blog/app.env`（权限 600，含 DB 密码 / JWT 密钥 / AI Key） |
| 上传文件 | `/opt/spring-blog/data/uploads` |
| 业务日志 | `/opt/spring-blog/logs/{app,operation}` |
| systemd | `spring-blog.service`（已 enabled，开机自启，`Restart=on-failure`） |
| JVM 参数 | `-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m` |

## 首次部署做了什么

```bash
# 1. 本地打包
./mvnw.cmd -DskipTests clean package

# 2. 上传（脚本 + jar）
scp deploy/server-setup.sh app.jar root@118.31.186.244:/root/

# 3. 服务器初始化（装 JDK17/MySQL、建库、生成 app.env、注册 systemd）
bash /root/server-setup.sh 118.31.186.244
```

## 日常更新（改代码后重新发布）

```bash
# 本地
./mvnw.cmd -DskipTests clean package
scp target/Martial-Arts-Ascension-Blog-0.0.1-SNAPSHOT.jar root@118.31.186.244:/opt/spring-blog/app.jar

# 服务器
ssh root@118.31.186.244 'systemctl restart spring-blog && systemctl status spring-blog --no-pager'
```

## 运维命令

```bash
systemctl status spring-blog --no-pager       # 服务状态
journalctl -u spring-blog -f                  # 实时控制台日志
tail -f /opt/spring-blog/logs/app/*.log       # 业务日志
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/   # 本机探活

# 改配置后生效（app.env 是环境变量源，改完必须 restart）
vim /opt/spring-blog/app.env
systemctl restart spring-blog
```

## 环境变量（`/opt/spring-blog/app.env`）

| 变量 | 说明 |
| --- | --- |
| `SERVER_PORT` | 覆盖 `server.port` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 数据源 |
| `JWT_SECRET` | 随机 56 位，泄露等同可伪造登录态 |
| `AI_API_KEY` | DeepSeek Key；为 `not-set` 时 AI 对话不可用，其余功能正常 |
| `CORS_ORIGINS` | 允许的前端来源，逗号分隔 |
| `FILE_UPLOAD_DIR` | 覆盖 `file.upload-dir` |
| `LOG_HOME` / `LOG_LEVEL_APP` | 日志目录 / 业务包日志级别（生产用 INFO） |

## 已知待办

- **安全组**：控制台入方向需放行 `8081/tcp`，否则公网打不开（当前仅 22 端口可达）。
- **AI 对话**：`AI_API_KEY` 仍为 `not-set`，填入 DeepSeek Key 并重启后可用。
- **HTTPS**：如需域名 + 证书，可在前面加一层 Nginx 反代（`deploy/nginx-spring-blog.conf` 已备），并放行 80/443。

## 排障

| 现象 | 排查 |
| --- | --- |
| 502/打不开 | `systemctl status spring-blog` → `journalctl -u spring-blog -n 100` |
| 启动报数据库连接失败 | `systemctl status mysqld`；核对 `app.env` 里 `DB_PASSWORD` 与 `mysql -ublog -p... blog` |
| 上传头像失败 | `ls -ld /opt/spring-blog/data/uploads` 权限应为 `springblog` |
| 内存吃紧 | `free -m`；heap 上限已限 512M，必要时降到 `-Xmx384m` |
