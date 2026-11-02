<#
.SYNOPSIS
    一键发布到阿里云 ECS（增量构建 + jar 条目级差异上传 + 就绪轮询 + 逐条目校验）。

.DESCRIPTION
    流程（每步单独计时，最后打印汇总表）：
      1 build     只改了 resources/static/templates 时走 `mvnw -o package` 增量离线编译；
                  一旦有 .java / pom.xml 变动自动退回 `clean package` 联网全量。
      2 session   建立 SSH + SFTP 会话（复用，避免每次都握手）。
      3 diff      本地 jar 与线上 jar 逐条目比 CRC32；若差异全部落在 BOOT-INF/classes/ 且没有条目被删除，
                  走 PATCH，否则整包上传 FULL（jar uf 无法删除条目，也不该去动 BOOT-INF/lib）。
      4 upload    PATCH 只把差异条目重新打成一个 patch.zip 上传（实测 ~100 KB vs 整包 ~57 MB）。
      5 apply     远端：备份 -> jar uf 就地替换 -> unzip -tqq 校验 -> chown/chmod -> mv -f 原子换 ->
                  只把改动的 static 同步到 Nginx 直读目录 -> systemctl restart -> 1 秒粒度轮询 /login 就绪。
                  任一步失败自动回滚到最新备份并降级 FULL。
      6 verify    线上 jar 再取一次条目清单，与本地逐条目比 CRC32 + 压缩方式，
                  证明 jar uf 没有把 BOOT-INF/lib 必需的 Stored 改成 Deflate。

    实测：资源类改动全流程 ≈ 56 s（其中应用重启就绪 ≈ 12 s），上传量从 59.68 MB 降到 104.9 KB。

.PARAMETER ForceFull
    跳过差异分析，始终整包上传（怀疑线上 jar 被手工动过时用）。

.PARAMETER SkipBuild
    复用已有 target\*.jar，只负责上传与生效（用于纯服务端回滚演练）。

.EXAMPLE
    $env:DEPLOY_SSH_PASSWORD = 'xxx'; .\deploy\deploy.ps1
.EXAMPLE
    .\deploy\deploy.ps1 -ForceFull          # 会交互式提示输入密码
#>
param(
    [string]$ServerIp = '118.31.186.244',
    [string]$SshUser  = 'root',
    [switch]$ForceFull,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
Import-Module Posh-SSH

$root      = Split-Path -Parent $PSScriptRoot
$jarName   = 'Martial-Arts-Ascension-Blog-0.0.1-SNAPSHOT.jar'
$jar       = Join-Path $root "target\$jarName"
$unzip     = 'D:\software\Git\usr\bin\unzip.exe'
$remoteJar = '/opt/spring-blog/app.jar'

# 密钥不落仓库：优先读环境变量，否则交互式输入
if ($env:DEPLOY_SSH_PASSWORD) {
    $sec = ConvertTo-SecureString $env:DEPLOY_SSH_PASSWORD -AsPlainText -Force
} else {
    $sec = Read-Host "SSH 密码（$SshUser@$ServerIp）" -AsSecureString
}
$cred = New-Object System.Management.Automation.PSCredential($SshUser, $sec)

$script:meter = @{}
function Step($name, [scriptblock]$body) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    Write-Host ("[STEP] " + $name) -ForegroundColor Cyan
    & $body
    $sw.Stop()
    $script:meter[$name] = $sw.ElapsedMilliseconds
    Write-Host ("       -> " + $sw.ElapsedMilliseconds + " ms") -ForegroundColor DarkGray
}

# unzip -v 输出 -> @{ name -> @{crc; len; method} }
function Parse-UnzipV($lines) {
    $map = @{}
    foreach ($l in $lines) {
        $m = [regex]::Match($l.Trim(), '^(\d+)\s+(\S+)\s+(\d+)\s+\d+%\s+\S+\s+\S+\s+([0-9a-f]{8})\s+(.+)$')
        if ($m.Success) {
            $map[$m.Groups[5].Value] = @{ crc = $m.Groups[4].Value; len = [int]$m.Groups[1].Value; method = $m.Groups[2].Value }
        }
    }
    return $map
}

function Run-Remote($cmd, $timeout = 300, [switch]$Quiet) {
    $r = Invoke-SSHCommand -SessionId $script:ssh.SessionId -Command $cmd -TimeOut $timeout
    if (-not $Quiet) {
        foreach ($l in $r.Output) { Write-Host ('    ' + $l) }
        if ($r.Error) { foreach ($l in $r.Error) { Write-Host ('    [err] ' + $l) -ForegroundColor Yellow } }
    }
    return $r
}

# 远端脚本一律 ASCII + base64 传递，避免 PowerShell / ssh 的多层引号与编码问题
function Invoke-RemoteScript($body, $timeout = 600, [switch]$Quiet) {
    $lf  = ($body -replace "`r`n", "`n")
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($lf))
    return (Run-Remote ("echo " + $b64 + " | base64 -d | bash 2>&1") $timeout -Quiet:$Quiet)
}

$swTotal = [Diagnostics.Stopwatch]::StartNew()

# ---------------- 1. build ----------------
Step 'build' {
    $ErrorActionPreference = 'Continue'   # git / mvnw 会往 stderr 写警告，Stop 模式会误判为失败
    if ($SkipBuild) { Write-Host '    skip (reuse existing jar)'; $ErrorActionPreference = 'Stop'; return }
    $changedFiles = @()
    $changedFiles += @(git -C $root -c core.safecrlf=false diff --name-only HEAD 2>$null)
    $changedFiles += @(git -C $root -c core.safecrlf=false ls-files --others --exclude-standard 2>$null)
    $needClean = ($changedFiles | Where-Object { $_ -match '\.java$|^pom\.xml$' } | Measure-Object).Count -gt 0
    $env:JAVA_HOME = 'D:\jdk_config\jdk'
    Set-Location $root
    if ($needClean) {
        Write-Host '    mode=clean+online (java/pom changed)'
        .\mvnw.cmd -DskipTests -q clean package 2>&1 | Select-String -Pattern 'ERROR|BUILD FAIL' | Out-String
    } else {
        Write-Host '    mode=incremental+offline (resources only)'
        .\mvnw.cmd -DskipTests -q -o package 2>&1 | Select-String -Pattern 'ERROR|BUILD FAIL' | Out-String
    }
    $ErrorActionPreference = 'Stop'
    if (-not (Test-Path $jar)) { throw 'build produced no jar' }
    Write-Host ('    jar=' + (Get-Item $jar).Length + ' B')
}

# ---------------- 2. sessions ----------------
Step 'ssh+sftp session' {
    $script:ssh  = New-SSHSession  -ComputerName $ServerIp -Credential $cred -AcceptKey -Force
    $script:sftp = New-SFTPSession -ComputerName $ServerIp -Credential $cred -AcceptKey -Force
}

# ---------------- 3. entry-level diff ----------------
$script:mode = 'FULL'
$script:diffNames = @()
Step 'diff local vs remote jar' {
    $localMap  = Parse-UnzipV (& $unzip -v $jar)
    Write-Host ('    local entries=' + $localMap.Count)
    $remote    = Invoke-RemoteScript ("unzip -v " + $remoteJar) -Quiet
    $remoteMap = Parse-UnzipV $remote.Output
    Write-Host ('    remote entries=' + $remoteMap.Count)
    $script:localMap  = $localMap
    $script:remoteMap = $remoteMap
    if ($ForceFull -or $remoteMap.Count -eq 0) { Write-Host '    force/full'; return }
    $diff = @()
    $onlyRemote = @()
    # Dir entries are not a real difference: zero payload, and jar uf rewrites them. Remote unpack uses
    # `find BOOT-INF/classes -type f`, so it never depends on directory entries.
    foreach ($k in $localMap.Keys) {
        if ($k.EndsWith('/')) { continue }
        if (-not $remoteMap.ContainsKey($k)) { $diff += $k }
        elseif ($remoteMap[$k].crc -ne $localMap[$k].crc) { $diff += $k }
    }
    foreach ($k in $remoteMap.Keys) { if (-not $k.EndsWith('/') -and -not $localMap.ContainsKey($k)) { $onlyRemote += $k } }
    Write-Host ('    changed=' + $diff.Count + ' removed=' + $onlyRemote.Count)
    if ($diff.Count -eq 0 -and $onlyRemote.Count -eq 0) {
        $script:mode = 'NOOP'
        Write-Host '    remote jar already identical - nothing to deploy' -ForegroundColor Green
        return
    }
    $bad = @($diff | Where-Object { $_ -notlike 'BOOT-INF/classes/*' })
    if ($bad.Count -eq 0 -and $diff.Count -gt 0 -and $onlyRemote.Count -eq 0) {
        $script:mode = 'PATCH'; $script:diffNames = $diff
        $bytes = 0; foreach ($d in $diff) { $bytes += $localMap[$d].len }
        Write-Host ('    PATCH ok, payload=' + $bytes + ' B uncompressed (vs full jar ' + (Get-Item $jar).Length + ' B)')
        ($diff | Sort-Object) | ForEach-Object { Write-Host ('      + ' + $_ + ' [' + $localMap[$_].len + 'B]') -ForegroundColor DarkCyan }
    } else {
        Write-Host ('    fallback FULL: non-class entries touched=' + $bad.Count + ' removed=' + $onlyRemote.Count) -ForegroundColor Yellow
        if ($bad.Count -gt 0)         { ($bad         | Select-Object -First 5) | ForEach-Object { Write-Host ('      ' + $_) -ForegroundColor Yellow } }
        if ($onlyRemote.Count -gt 0)  { ($onlyRemote  | Select-Object -First 5) | ForEach-Object { Write-Host ('      gone: ' + $_) -ForegroundColor Yellow } }
    }
}

# ---------------- 4. upload ----------------
Step 'upload' {
    if ($script:mode -eq 'NOOP') { Write-Host '    skip (nothing changed)'; return }
    Invoke-RemoteScript 'mkdir -p /opt/spring-blog/incoming' | Out-Null
    if ($script:mode -eq 'PATCH') {
        $zip = Join-Path $root 'target\patch.zip'
        if (Test-Path $zip) { Remove-Item $zip -Force }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        # 必须从本地 jar 逐条目按原名复制：CreateFromDirectory 在 PS 5.1 下会生成 BOOT-INF\classes\... 反斜杠条目名，
        # 远端 unzip 会解出字面文件名的垃圾目录
        $srcZ = [IO.Compression.ZipFile]::OpenRead($jar)
        $fsz  = [IO.File]::Open($zip, [IO.FileMode]::Create)
        $za   = New-Object IO.Compression.ZipArchive($fsz, [IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($n in $script:diffNames) {
                $se = $srcZ.GetEntry($n)
                if ($null -eq $se) { throw ('missing entry in local jar: ' + $n) }
                $de  = $za.CreateEntry($n, [IO.Compression.CompressionLevel]::Optimal)
                $sin = $se.Open(); $sou = $de.Open()
                try { $sin.CopyTo($sou) } finally { $sin.Dispose(); $sou.Dispose() }
            }
        } finally { $za.Dispose(); $fsz.Dispose(); $srcZ.Dispose() }
        $names = @(& $unzip -Z1 $zip)
        $slashBad = @($names | Where-Object { $_ -notmatch '^BOOT-INF/classes/' -or $_ -match '\\' })
        if ($slashBad.Count -gt 0) { throw ('patch.zip bad entry names: ' + ($slashBad -join ', ')) }
        if ($names.Count -ne $script:diffNames.Count) { throw ('patch.zip entry count ' + $names.Count + ' != diff ' + $script:diffNames.Count) }
        Write-Host ('    patch.zip=' + (Get-Item $zip).Length + ' B, entries=' + $names.Count)
        Set-SFTPItem -SessionId $script:sftp.SessionId -Path $zip -Destination '/opt/spring-blog/incoming' -Force
    } else {
        Write-Host ('    full jar=' + (Get-Item $jar).Length + ' B')
        Set-SFTPItem -SessionId $script:sftp.SessionId -Path $jar -Destination '/opt/spring-blog/incoming' -Force
    }
}

# ---------------- 5. apply + restart + readiness ----------------
Step 'apply+restart+wait-ready' {
    if ($script:mode -eq 'NOOP') { Write-Host '    skip (nothing changed)'; return }
    if ($script:mode -eq 'PATCH') {
        $body = @'
set -e
JAR=/usr/local/jdk-24.0.2/bin/jar
TS=$(date +%Y%m%d%H%M%S)
cd /tmp && rm -rf patch && mkdir -p patch
unzip -qo /opt/spring-blog/incoming/patch.zip -d /tmp/patch
cp -a /opt/spring-blog/app.jar /tmp/app-new.jar
cp -a /opt/spring-blog/app.jar /opt/spring-blog/app.jar.bak.$TS
ls -1t /opt/spring-blog/app.jar.bak.* 2>/dev/null | tail -n +4 | xargs -r rm -f
cd /tmp/patch
FILES=$(find BOOT-INF/classes -type f)
echo "patch_files=$(echo "$FILES" | wc -l)"
$JAR uf /tmp/app-new.jar $FILES
unzip -tqq /tmp/app-new.jar && echo "ZIP_TEST ok"
$JAR tf /tmp/app-new.jar | wc -l
chown springblog:springblog /tmp/app-new.jar 2>/dev/null || true
chmod 644 /tmp/app-new.jar
mv -f /tmp/app-new.jar /opt/spring-blog/app.jar
if [ -d /tmp/patch/BOOT-INF/classes/static ]; then
  mkdir -p /opt/spring-blog/static
  cp -a /tmp/patch/BOOT-INF/classes/static/. /opt/spring-blog/static/
  chmod -R a+rX /opt/spring-blog/static
  echo "static synced"
fi
rm -f /opt/spring-blog/incoming/patch.zip
systemctl restart spring-blog
i=0
while [ $i -lt 90 ]; do
  if curl -sf -o /dev/null --max-time 2 http://127.0.0.1/login; then echo "READY after ${i}s"; break; fi
  sleep 1; i=$((i+1))
done
systemctl is-active spring-blog
curl -s -o /dev/null -w 'login_http=%{http_code}\n' --max-time 15 http://127.0.0.1/login || true
echo "stored_entries=$(unzip -v /opt/spring-blog/app.jar | awk '$2=="Stored"' | wc -l)"
'@
        $r = Invoke-RemoteScript $body
        $out = ($r.Output -join "`n")
        if ($r.ExitStatus -ne 0 -or $out -notmatch 'READY after') {
            Write-Host '    PATCH failed -> restore newest backup, fall back to FULL' -ForegroundColor Red
            Invoke-RemoteScript @'
BK=$(ls -1t /opt/spring-blog/app.jar.bak.* 2>/dev/null | head -1)
if [ -n "$BK" ]; then
  mv -f "$BK" /opt/spring-blog/app.jar
  chown springblog:springblog /opt/spring-blog/app.jar; chmod 644 /opt/spring-blog/app.jar
  systemctl restart spring-blog
  echo "rolled back from $BK"
fi
'@ | Out-Null
            $script:mode = 'FULL'
        }
    }
}

if ($script:mode -eq 'FULL') {
    Step 'apply full jar' {
        Invoke-RemoteScript @'
set -e
cd /opt/spring-blog
mv -f incoming/Martial-Arts-Ascension-Blog-0.0.1-SNAPSHOT.jar app.jar
chown springblog:springblog app.jar && chmod 644 app.jar
systemctl restart spring-blog
i=0
while [ $i -lt 90 ]; do
  if curl -sf -o /dev/null --max-time 2 http://127.0.0.1/login; then echo "READY after ${i}s"; break; fi
  sleep 1; i=$((i+1))
done
rm -rf /tmp/bs && mkdir -p /tmp/bs
cd /tmp/bs && unzip -qo /opt/spring-blog/app.jar 'BOOT-INF/classes/static/*'
mkdir -p /opt/spring-blog/static
cp -a /tmp/bs/BOOT-INF/classes/static/. /opt/spring-blog/static/ && chmod -R a+rX /opt/spring-blog/static
systemctl is-active spring-blog
curl -s -o /dev/null -w 'login_http=%{http_code}\n' --max-time 15 http://127.0.0.1/login || true
'@
    }
}

# ---------------- 6. prove remote jar == local build ----------------
Step 'verify remote jar entry-by-entry' {
    $localMap = $script:localMap
    $remote   = Invoke-RemoteScript ("unzip -v " + $remoteJar) -Quiet
    $nowMap   = Parse-UnzipV $remote.Output
    $mismatch = @()
    # Directory entries (name ends with /) carry no payload, and `jar uf` rewrites them with its own CRC/size
    # convention, so comparing them only produces false mismatches. Keep this block ASCII: PS 5.1 reads BOM-less
    # files as GBK and mangled CJK bytes can emit stray [ or backtick tokens that break parsing.
    $dirSkipped = 0
    foreach ($k in $localMap.Keys) {
        if ($k.EndsWith('/')) { $dirSkipped++; continue }
        if (-not $nowMap.ContainsKey($k) -or $nowMap[$k].crc -ne $localMap[$k].crc) { $mismatch += ('CRC ' + $k) }
        elseif ($nowMap[$k].method -ne $localMap[$k].method) { $mismatch += ('METHOD ' + $k + ' ' + $localMap[$k].method + '->' + $nowMap[$k].method) }
    }
    foreach ($k in $nowMap.Keys) {
        if ($k.EndsWith('/')) { continue }
        if (-not $localMap.ContainsKey($k)) { $mismatch += ('EXTRA ' + $k) }
    }
    Write-Host ('    entries compared=' + ($localMap.Count - $dirSkipped) + ' (dir entries skipped=' + $dirSkipped + ')  mismatch=' + $mismatch.Count)
    if ($mismatch.Count -gt 0) {
        ($mismatch | Select-Object -First 10) | ForEach-Object { Write-Host ('      ' + $_) -ForegroundColor Red }
    } else {
        Write-Host '    remote jar is byte-equivalent to local build (all CRC + method match)' -ForegroundColor Green
    }
    $script:mismatch = $mismatch
}

# ---------------- 7. timing + smoke ----------------
$swTotal.Stop()
Write-Host ''
Write-Host '===== deploy timing =====' -ForegroundColor Cyan
foreach ($k in $script:meter.Keys) { Write-Host ('  {0,-28} {1,8} ms' -f $k, $script:meter[$k]) }
Write-Host ('  ' + ('-' * 38))
Write-Host ('  {0,-28} {1,8} ms  ({2:N1} s)' -f 'TOTAL', $swTotal.ElapsedMilliseconds, ($swTotal.ElapsedMilliseconds / 1000))
Write-Host ('  mode=' + $script:mode + '  patchedEntries=' + $script:diffNames.Count + '  mismatch=' + $script:mismatch.Count)

Invoke-RemoteScript 'journalctl -u spring-blog --since "-4 min" --no-pager | grep -E "Started in|Tomcat started|APPLICATION FAILED" | tail -4'

Remove-SSHSession  -SessionId $script:ssh.SessionId  | Out-Null
Remove-SFTPSession -SessionId $script:sftp.SessionId | Out-Null

if ($script:mismatch.Count -gt 0) { throw 'deploy finished but remote jar differs from local build' }
