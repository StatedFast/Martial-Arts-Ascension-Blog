# 裸公网 IP 的 HTTPS 证书方案（无域名、无 ICP 备案）

> 适用场景：只有一台云服务器 + 公网 IP，没有域名或域名无法备案，又想让浏览器真正信任锁标（而不是自签名点"继续访问"）。
> 本文是本项目在 118.31.186.244 上的**实际落地记录**，命令与路径均可直接复制复现。

---

## 一、最终生效的是什么

| 项 | 值 |
| --- | --- |
| CA | Let's Encrypt（公共 CA，浏览器内置信任） |
| 签发对象 | **IP 地址本身**，不是域名 |
| 证书身份 | `subject` 为空属正常，身份写在 `SAN (Subject Alternative Name)` 的 `IP Address:118.31.186.244` 扩展里，且该扩展 `critical` |
| 密钥算法 | ECDSA `ec-256`（比 RSA 2048 更快、证书更小） |
| 有效期 | **约 6.6 天**（IP 证书被 CA 强制走短效档） |
| 客户端 | acme.sh v3.1.7，装在 `/root/.acme.sh` |
| 验证方式 | `http-01`（CA 通过 80 端口取一个 token 文件来证明"这台机器的确归你"） |
| 私钥 / 全链 | `/etc/nginx/ssl/ip/privkey.pem`、`/etc/nginx/ssl/ip/fullchain.pem` |
| 验证站 | `/var/www/letsencrypt/.well-known/acme-challenge/`（webroot） |
| TLS 终止 | Nginx 1.24 监听 443，反代到应用 8090（`server.port` 保持明文，只监听 127.0.0.1） |
| 自动续期 | `/etc/cron.d/acme-renew` 每天 `04:17` 与 `16:17` 跑一次 `acme.sh --cron`，续到就 `reload nginx` |

---

## 二、分步实现

### 步骤 0：先在云控制台放行 443

`http-01` 依赖 **80 端口公网可达**，业务访问依赖 **443**。这两个端口必须在**阿里云安全组**入方向放行 —— 安全组在服务器之外，机器上任何命令都改不动它，这是唯一一步必须人工在控制台做的。

### 步骤 1：装 ACME 客户端（为什么不用 certbot）

系统包管理器里的 `certbot` 是 **1.22.0**，太老，**不支持给 IP 地址签发**（该能力要 5.3+）。改用 `acme.sh` 的 master 分支，它支持 `--certificate-profile shortlived`（IP 证书专用档案）。

GitHub 直连被连接重置时，走 ghproxy 前缀镜像拉 tar 包：

```bash
curl -L -o /tmp/acme.tar.gz https://mirror.ghproxy.com/https://github.com/acmesh-official/acme.sh/archive/master.tar.gz
tar xzf /tmp/acme.tar.gz -C /tmp && cd /tmp/acme.sh-master && ./acme.sh install
```

⚠️ `--install` **必须先 `cd` 进源码目录**再执行，否则报 `cannot stat 'acme.sh'`（它是相对路径安装）。

### 步骤 2：在 80 的 server 块挖一个 ACME 挑战目录

编辑 `/etc/nginx/conf.d/spring-blog-80.conf`，加：

```nginx
location ^~ /.well-known/acme-challenge/ {
    root /var/www/letsencrypt;      # 注意是 root，不是 alias
    default_type "text/plain";
}
```

建目录并重载：

```bash
mkdir -p /var/www/letsencrypt/.well-known/acme-challenge
nginx -t && systemctl reload nginx
```

⚠️ **token 必须放在 `<webroot>/.well-known/acme-challenge/` 下**。Nginx 的 `root` 会把完整 URI 拼在后面，写成 `root .../acme-challenge` 就会去找 `.well-known/acme-challenge/.well-known/acme-challenge/<token>` → 404 → 签发失败。这是整个流程最容易翻车的一步。

### 步骤 3：签发

```bash
acme.sh --issue -d 118.31.186.244 \
  -w /var/www/letsencrypt \
  --server letsencrypt \
  --keylength ec-256 \
  --certificate-profile shortlived \
  --days 3
```

- `-d` 直接写 **IP**（acme.sh 会识别成 IP 证书，走 `ip` SAN）；
- `--certificate-profile shortlived`：签给 IP 必须用短效档；
- `--days 3`：有效期不足 3 天就续，配合 6.6 天的短证书。

⚠️ **邮箱别填成 IP**。如果之前注册过账户且 contact 邮箱不合法，Let's Encrypt 会 `invalidContact`；这里用**无 contact 的账户**完成签发（会跳过邮箱校验，代价是续费提醒邮件收不到，靠 cron 日志兜底）。

### 步骤 4：安装证书 + 绑定 reload

```bash
mkdir -p /etc/nginx/ssl/ip
acme.sh --install-cert -d 118.31.186.244 --ecc \
  --key-file       /etc/nginx/ssl/ip/privkey.pem \
  --fullchain-file /etc/nginx/ssl/ip/fullchain.pem \
  --reloadcmd      "nginx -t && systemctl reload nginx"
```

`--install-cert` 的作用是把证书"部署到指定路径"，并**每次续期后自动执行 `--reloadcmd`**。之后证书就归 acme.sh 管了，手工去改那两个 pem 文件会在下次续期时被覆盖。

### 步骤 5：写 443 的 server 块

留档文件 [`deploy/nginx-spring-blog-https.conf`](./nginx-spring-blog-https.conf) → 拷到 `/etc/nginx/conf.d/spring-blog-https.conf`。两个坑：

- Nginx **< 1.25 用 `listen 443 ssl http2;`**（写成单独的 `http2 on;` 老版本不认）；
- **`upstream spring_blog` 只能定义一次**，它已经在 `spring-blog.conf` 里，443 块**只 `proxy_pass`，绝不重复声明 upstream**，否则 `nginx -t` 直接 `duplicate "upstream"`；同一个 `listen` 指令也不可重复出现。

### 步骤 6：配自动续期

```bash
cat > /etc/cron.d/acme-renew <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
17 4,16 * * * root /root/.acme.sh/acme.sh --cron --home /root/.acme.sh >>/var/log/acme-cron.log 2>&1
EOF
```

`17 4,16` 是**错开整点**（避开大量人用的 0 点档，也避开 CA 速率高峰），每天查两次，够阈值才真正续。

### 步骤 7：实测一次续期链路（不能只配不验）

配完 cron 不等于会用。强制走一遍完整续期：

```bash
acme.sh --renew -d 118.31.186.244 --ecc --force
openssl x509 -in /etc/nginx/ssl/ip/fullchain.pem -noout -dates -ext subjectAltName
```

看到 `notAfter` **往后推了**，才算闭环。**IP 短效证书一旦自动续期失败，6.6 天后全站变红锁** —— 这是本项目里唯一"配置丢了就整站挂"的机制，必须验。

### 步骤 8：验收

```bash
# 本地：看证书身份与有效期
openssl x509 -in /etc/nginx/ssl/ip/fullchain.pem -noout -subject -issuer -dates -ext subjectAltName
# 期望：subject=（空）、issuer=Let's Encrypt、SAN=critical IP Address:118.31.186.244、有效期 +6 天左右
```

从外网验收要用**默认严格校验证书**的客户端，不要 `curl -k`（`-k` 会跳过校验，自签名也照样 200，等于没验）：

```powershell
Invoke-WebRequest https://118.31.186.244/login            # PS 默认严格校验
```

浏览器看锁标点进去，签发者应显示 `Let's Encrypt`，而非"不安全/自签名"。

---

## 三、和企业级生产环境的区别

核心差别只有一句话：**这里是在给"一台机器"签证书，企业是在给"一个服务身份/域名"签证书，并且证书只是整条安全链路里最末梢的一环。** 展开：

| 维度 | 本方案（裸 IP 单实例） | 企业级生产 |
| --- | --- | --- |
| **身份锚点** | IP 地址，写死一个 | 域名 / 泛域名（`*.example.com`），与机器解耦 |
| **换机器 / 扩容** | **IP 一变证书就作废**，必须重新签发 | 加机器不用动证书，弹性扩容、蓝绿无感 |
| **验证方式** | `http-01`，必须**公网开 80**、把挑战目录暴露出去 | 主用 `DNS-01`：加一条 TXT 记录即可，不用暴露端口，能签**内网域名和泛域名** |
| **CA 与信任根** | 公共 CA（Let's Encrypt），信任链靠浏览器内置根 | 自建 **私有 PKI / 内部根 CA**，服务间证书由内部根签发，根证书推到各机器信任库；对外仍用公共 CA |
| **证书生命周期** | 6.6 天，靠机器上的 cron 手动续 | 集中式证书平台（企业通常有**证书管理系统/台账**），统一申请、分发、吊销、到期告警；证书到期是 P0 事故，有专人盯 |
| **私钥保护** | 私钥落 `/etc/nginx/ssl/ip/privkey.pem`，权限靠文件系统 | **私钥进 KMS / HSM，出不了硬件**；或走 ACME 时私钥只在内存；运维拿不到明文私钥，满足审计 |
| **TLS 终止位置** | Nginx 单机终止，回源 8090 明文 | **CDN / SLB / WAF 边缘终止**，多入口多处证书，回源走 mTLS 或专线；源站证书由内部 CA 管 |
| **加固基线** | `TLSv1.2/1.3` + `HIGH:!aNULL:!MD5`（够用即可，没做精细基线） | **HSTS + 预加载、OCSP stapling、仅 TLS1.3、指定强密码套件组、禁用压缩**等安全基线，且定期被扫描打合规报告 |
| **域名合规** | 用 IP 直连**绕开了 ICP 备案**（国内域名对外提供 80/443 服务需备案） | 域名 + 备案 + 公安联网，这是硬合规项 |
| **吊销与应急** | 没有 OCSP/CRL 主动管理，靠续期覆盖 | 私钥疑似泄露时**分钟级吊销 + 全链路替换**，有应急预案 |
| **可观测** | 只有 cron 日志（`/var/log/acme-cron.log`），失败靠人翻 | 证书剩余天数接入监控大盘 + 分级告警，续期失败自动工单 |
| **合规审计** | 无 | 等保 / ISO27001 / SOC2 / PCI 等对证书类型、密钥长度、轮换周期、访问留痕都有明确条款 |

**换句话说**，本方案适合**个人项目、演示环境、内部临时服务、面试作品集**：成本几乎为零、全自动、浏览器确实信任。它的软肋也很清楚——和单一 IP 强绑定、依赖机器本地 cron、私钥躺在磁盘上、`http-01` 要求公网暴露 80。

一旦满足下面任一条件，就该换成"域名 + DNS-01 + 证书集中托管（对外 LE/商业 CA，内部私有 CA）"：
1. 要**弹性扩容 / 换机器 / 上 CDN**；
2. 有**移动端 App 固定域名**或要被搜索引擎收录（IP 直连基本不进索引，且国内域名对外需 ICP 备案）；
3. 服务间调用要**双向 mTLS / 内部私有 CA**；
4. 要过**等保 / 安全审计**。
