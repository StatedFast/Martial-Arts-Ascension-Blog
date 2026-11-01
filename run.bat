@echo off
chcp 65001 >nul
echo ====================================
echo   Spring Blog 启动中...
echo   控制台编码已设置为 UTF-8
echo ====================================
REM ----------------------------------------------------------------------
REM OOM 故障演练参数（配合「配置中心」的 fault.oom.* 开关使用）：
REM   -Xmx256m -Xms256m            小堆，让泄漏几分钟内就能打满，模拟小容器
REM   -XX:MaxDirectMemorySize=64m  堆外上限，direct 类型故障才会收敛为 OOM（否则默认等于 -Xmx，很难触发）
REM   -XX:+HeapDumpOnOutOfMemoryError  OOM 时自动 dump，用于 MAT/JProfiler 分析
REM   -XX:HeapDumpPath=.\logs      dump 落地目录
REM   -Xlog:gc*:file=.\logs\gc.log  GC 日志，观察 Full GC 频率与老年代回收率
REM 不想演练时把下面整段 jvmArguments 去掉即可恢复常态启动。
REM ----------------------------------------------------------------------
call .\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Xms256m -Xmx256m -XX:MaxDirectMemorySize=64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs -Xlog:gc*:file=./logs/gc.log:time,uptime:filecount=5,filesize=5m"
