# 部署说明（阿里云 ECS）

## 服务器

| 项 | 值 |
| --- | --- |
| 公网 IP | 118.31.186.244 |
| 系统 | Alibaba Cloud Linux 3 (Anolis 8) |
| 规格 | 2 vCPU / 1.8 GiB / 40 GiB ESSD / 4 GiB swap |
| 运行用户 | `springblog`（nologin 系统用户） |
| 应用目录 | `/opt/spring-blog` |
| 对外端口 | `80` 与 `8081`（由 Nginx 监听） |
| 应用端口 | `8090`（仅监听本机，由 Nginx 反代） |
| 入口地址 | `http://118.31.186.244/` 或 `http://118.31.186.244:8081/` |

## 服务端组成

| 组件 | 版本 / 位置 |
| --- | --- |
| JDK | OpenJDK 17（`/usr/lib/jvm/java-17-openjdk-*`，显式指定，不受机器上 JDK 24 影响） |
| MySQL | 8.0.46，库 `blog`，账号 `blog@localhost`，密码随机生成在 `app.env` |
| MySQL 调优 | `/etc/my.cnf.d/zz-blog-small.cnf`（buffer pool 128M、关闭 performance_schema，适配 2G 内存） |
| 应用 jar | `/opt/spring-blog/app.jar` |
| Nginx | 1.24，配置 `/etc/nginx/conf.d/spring-blog.conf`（8081）与 `spring-blog-80.conf`（80） |
| 静态资源 | `/opt/spring-blog/static/{css,js,images}`（从 jar 抽出，Nginx 直接读盘） |
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

# 服务器：先传成 .new，再原子替换（避免运行中进程读不到类）
scp target/Martial-Arts-Ascension-Blog-0.0.1-SNAPSHOT.jar root@118.31.186.244:/opt/spring-blog/app.jar.new
ssh root@118.31.186.244 '
  chown springblog:springblog /opt/spring-blog/app.jar.new &&
  chmod 644 /opt/spring-blog/app.jar.new &&
  mv /opt/spring-blog/app.jar.new /opt/spring-blog/app.jar &&
  systemctl restart spring-blog'
```

**注意：如果这次改动涉及 `src/main/resources/static/` 下的 CSS/JS/图片，必须同步刷新 Nginx 的静态目录**，否则页面还是旧资源（Nginx 直接读 `/opt/spring-blog/static`，不读 jar）：

```bash
ssh root@118.31.186.244 '
  rm -rf /tmp/ext && mkdir -p /tmp/ext &&
  unzip -q /opt/spring-blog/app.jar "BOOT-INF/classes/static/*" -d /tmp/ext &&
  rm -rf /opt/spring-blog/static && mkdir -p /opt/spring-blog/static &&
  cp -r /tmp/ext/BOOT-INF/classes/static/. /opt/spring-blog/static/ &&
  rm -rf /tmp/ext && chown -R springblog:springblog /opt/spring-blog/static'
```

## 静态资源优化（已生效）

`deploy/nginx-spring-blog.conf` 提供 gzip + 强缓存，实测传输体积：

| 资源 | 优化前 | 优化后 | 手段 |
| --- | --- | --- | --- |
| 首页 HTML | 20 KB | 5 KB | gzip |
| `style.css` | 65 KB | 14 KB | gzip |
| `app.js` | 24 KB | 7 KB | gzip |
| `home-songyu.jpg` | 3.1 MB | 299 KB | 重采样 3440×1440 → 2560×1072，JPEG q76 |
| 首屏合计 | ≈3.2 MB | ≈325 KB | — |

缓存策略：`/css` `/js` 7 天，`/images` 30 天 immutable，`/uploads` 7 天。

⚠️ 改图片/CSS 后如果文件名不变，用户浏览器可能命中旧缓存；版本号 `?v=` 已在模板里使用，改动静态文件时记得同步升版本号。

## 运维命令

```bash
systemctl status spring-blog --no-pager       # 应用状态
systemctl status nginx --no-pager             # Nginx 状态
journalctl -u spring-blog -f                  # 实时控制台日志
tail -f /opt/spring-blog/logs/app/*.log       # 业务日志
nginx -t && systemctl reload nginx            # 改 Nginx 配置后校验并生效
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/          # 经 Nginx 探活
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8090/     # 绕过 Nginx 直连应用

# 改配置后生效（app.env 是环境变量源，改完必须 restart）
vim /opt/spring-blog/app.env
systemctl restart spring-blog
```

## 环境变量（`/opt/spring-blog/app.env`）

| 变量 | 说明 |
| --- | --- |
| `SERVER_PORT` | 覆盖 `server.port`，当前为 `8090`（8081/80 由 Nginx 占用） |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 数据源 |
| `JWT_SECRET` | 随机 56 位，泄露等同可伪造登录态 |
| `AI_API_KEY` | DeepSeek Key；为 `not-set` 时 AI 对话不可用，其余功能正常 |
| `CORS_ORIGINS` | 允许的前端来源，逗号分隔 |
| `FILE_UPLOAD_DIR` | 覆盖 `file.upload-dir` |
| `LOG_HOME` / `LOG_LEVEL_APP` | 日志目录 / 业务包日志级别（生产用 INFO） |

## 已知待办

- **带宽计费方式**：当前公网带宽为「按使用流量」计费，2026-10-31 04:46 曾因余额不足被阿里云暂停带宽（现象：机器和本机 curl 都正常，但外网全不通，SSH 卡在 banner exchange）。建议在控制台改为「按固定带宽 5 Mbps」，避免再次因流量扣费导致全站不可用。
- **管理员默认密码**：种子账号 `admin` 的初始密码是弱口令，公网环境下必须尽快修改。
- **SSH 安全**：22 端口对全网开放且使用密码登录，建议改为密钥登录并把来源限制到固定 IP。
- **HTTPS**：如需域名 + 证书，可在现有 Nginx 上加 443 server 块（`deploy/nginx-spring-blog.conf` 已具备反代配置），并放行 443。注意国内 ECS 用域名对外服务需完成 ICP 备案。

## 排障

| 现象 | 排查 |
| --- | --- |
| 502/打不开 | `systemctl status nginx spring-blog` → `journalctl -u spring-blog -n 100` |
| 页面样式是旧的 | Nginx 静态目录没刷新，重新执行上面「刷新静态目录」命令 |
| 启动报数据库连接失败 | `systemctl status mysqld`；核对 `app.env` 里 `DB_PASSWORD` 与 `mysql -ublog -p... blog` |
| 上传头像失败 | `ls -ld /opt/spring-blog/data/uploads` 权限应为 `springblog` |
| 内存吃紧 | `free -m`；heap 上限已限 512M，必要时降到 `-Xmx384m` |
| 外网全不通、本机 curl 却正常 | 先查阿里云站内信/费用中心，多半是带宽欠费停服 |
