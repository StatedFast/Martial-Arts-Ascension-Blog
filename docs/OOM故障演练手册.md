# OOM 故障演练与排查手册（面试素材版）

> 配套「配置中心 → 故障演练开关」使用。本文件是给**自己看的台词本 + 实操命令小抄**，
> 所有命令都在本机 `run.bat` 启动的小堆环境（`-Xmx256m`）验证过。代码不提交也没关系，先跑通流程。

---

## 0. 这套演练在讲什么故事

一句话：**一次错误的"缓存优化"（无限增长的本地缓存 + 时间戳 key + 永不过期），导致生产服务运行数小时后进入 GC 死亡螺旋，最终堆内存 OOM。**

对应到本项目代码：
- 泄漏载体：`LocalCacheService`（裸 `ConcurrentHashMap`，无容量上限、无后台清理，`set` 永不过期）
- 触发器：`LeakSimulator` 定时任务，由 `blog_sys_config` 里的 `fault.oom.*` 开关驱动
- 修复版：`BoundedCacheService`（有界 LRU，`removeEldestEntry` 淘汰），`fault.oom.fixed=true` 时走它
- 两种故障：`fault.oom.type = heap`（堆内）/ `direct`（堆外直接内存）

---

## 1. 复现（制造"事故"）

### 1.1 启动参数（run.bat 已内置）
```bat
-Xms256m -Xmx256m                      :: 小堆，模拟小容器，让泄漏几分钟打满
-XX:MaxDirectMemorySize=64m            :: direct 故障才收敛得下来（默认等于 -Xmx，很难触发）
-XX:+HeapDumpOnOutOfMemoryError        :: OOM 时自动 dump，供 MAT 分析
-XX:HeapDumpPath=./logs                :: dump 落地目录
-Xlog:gc*:file=./logs/gc.log           :: GC 日志，观察老年代与 Full GC 趋势
```

### 1.2 打开演练开关
- 管理员登录 → 右上角头像下拉菜单 → **配置中心**（`/config`）
- 点「堆内泄漏(heap)」→ 点「开启演练」
- 盯页面顶部的 **JVM 实时内存条**：堆占用会阶梯上涨，Full GC 后回落越来越少 → 最终进程崩，`logs/` 下出现 `java_pid*.hprof`

### 1.3 命令行观察（另开一个终端）
```bash
jps -l                     # 找到 SpringAiDemoApplication 的 <pid>
jstat -gcutil <pid> 1000   # 每 1 秒打印一次各区占比与 GC 次数
```
`jstat -gcutil` 关键字段：
- `O`（老年代使用%）长期 > 90% 且不下降 = 泄漏信号
- `FGC`（Full GC 次数）快速增长、`FGCT`（Full GC 总耗时）暴涨 = GC 死亡螺旋
```bash
jmap -histo:live <pid> | head -20   # 存活对象直方图，Top 里若全是 byte[] / ConcurrentHashMap$Node / 你的业务对象 = 命中
```

---

## 2. 排查（定位到代码）

### 2.1 拿堆转储
```bash
# 要么用 OOM 自动生成的 ./logs/java_pid<pid>.hprof
# 要么进程还活着时手动抓：
jmap -dump:format=b,file=./logs/heap_<pid>.hprof <pid>
```

### 2.2 用 MAT（Eclipse Memory Analyzer）分析
1. `Open with Custom Query` / 直接打开 hprof
2. **Dominator Tree（支配树）**：按 `Retained Heap` 降序，第一通常是 `java.util.concurrent.ConcurrentHashMap` → 展开看到海量 `byte[](1MB)`，即 `LocalCacheService.cache`
3. 右键该 map → **Path to GC Roots → exclude weak/soft references**，引用链落到：
   ```
   LocalCacheService (Spring 单例 Bean)
     └─ ConcurrentHashMap cache
          └─ CacheEntry(value = byte[1048576])   ← 强引用、永不释放
   ```
   GC Root 是 Spring 容器持有的单例 Bean，所以这些对象**永远回收不掉**。
4. **Leak Suspects 报告**（打开文件时弹出的向导）会直接给出 "Problem Suspect 1: 一个 ConcurrentHashMap 占用了 xx% 堆"。

### 2.3 回到代码
- grep key 前缀 `article:snapshot:` → 命中 `LeakSimulator.injectHeap`
- 发现设计错误：key 带 `System.nanoTime()` 每条唯一 + 走永久 `set()` → 只进不出
- 确认 `LocalCacheService` 无容量上限、无淘汰

### 2.4 direct（堆外）类型的差异（面试延伸点）
- 症状不同：**堆使用率正常（O 区不涨）**，但进程 RSS 持续膨胀，最终 `OutOfMemoryError: Direct buffer memory`
- 观察口径：`MemoryMXBean` 的 nonHeap **照不到直接内存**，要用 `BufferPoolMXBean("direct")`（配置中心页面的「堆外直接内存」进度条 + 趋势曲线就是它，上限从启动参数解析 `-XX:MaxDirectMemorySize`，未设置时默认≈Xmx），或 `jcmd <pid> VM.native_memory` / 进程 RSS
- 页面面板：堆内（红）/ 堆外（蓝）双进度条 + 120 点趋势曲线 + 80% 告警参考线；direct 演练时应该看到**红线不动、蓝线上涨**，这是最快的定性判据
- 回收时机：摘掉 DirectByteBuffer 强引用后，native 内存不是立即归还，而是等 GC 标记到该 buffer 时由 Cleaner 释放 —— 所以回收接口会附带一次 `System.gc()`
- `jmap -histo` 看不到元凶（DirectByteBuffer 的堆内壳很小，真正内存在堆外）
- 定位靠 **NMT**：`-XX:NativeMemoryTracking=detail` → `jcmd <pid> VM.native_memory detail`，看 Internal/Other 段
- 或 MAT 里找 `java.nio.Bits` 的 `TOTAL_CAPACITY` / 大量 `DirectByteBuffer` 的 `cleaner`

---

## 3. 修复（分层，按生产处理顺序）

1. **止血**：关演练开关（或摘流量/重启），先恢复服务
2. **代码修复**（对应本项目）：
   - 缓存全改带 TTL；
   - 加**容量上限 + 淘汰**：`BoundedCacheService`（`LinkedHashMap` accessOrder LRU + `removeEldestEntry`），生产更推荐 **Caffeine**（`maximumSize` + `expireAfterWrite`）
   - 修掉带时间戳的 key 设计（同类请求应命中同一 key）
3. **验证对比闭环**：配置中心点「✓ 开启修复模式」→ 保持演练开启 → 内存条涨到 ~32MB（`fault.drill.bounded-cache-max-size`）即走平，页面显示「有界缓存：32 条 / 已淘汰 N 次」，证明淘汰在工作
   - ⚠️ **关键认知（面试必答）**：修复模式只能「止血」（不再新增泄漏），**不会让已泄漏的内存下降**——因为旧的 `byte[]` 仍被 `LocalCacheService` 这个单例 Bean 强引用，是活对象，GC 回收不掉。这正是「内存泄漏」的定义。
   - 想让曲线真正回落：点「↺ 回收已泄漏内存」（`POST /api/admin/config/recover` → `LeakSimulator.purgeHeapLeak()`），它摘掉泄漏 key 的强引用使其变为垃圾 + 触发一次 `System.gc()`，曲线随即下降。**生产上真正的止血手段是重启**，`System.gc()` 只为演示即时效果。
4. **防再犯**：
   - JVM 启动固定加 `HeapDumpOnOutOfMemoryError`
   - 监控告警：老年代使用率 >80% 持续 5 分钟告警
   - Code Review 规范：本地缓存必须有淘汰策略，禁止裸 Map 当缓存

---

## 4. 命令速查表

| 目的 | 命令 |
|---|---|
| 找进程 | `jps -l` / `jps -lv`（看启动参数） |
| 实时 GC/分区 | `jstat -gcutil <pid> 1000 100`（每 1s 一次共 100 次） |
| GC 原因 | `jstat -gccause <pid> 1000` |
| 存活对象直方图 | `jmap -histo:live <pid>` |
| 抓堆转储 | `jmap -dump:format=b,file=heap.hprof <pid>` |
| 堆/元空间概要 | `jcmd <pid> GC.heap_info` |
| 原生内存追踪 | `jcmd <pid> VM.native_memory detail`（需启动加 `-XX:NativeMemoryTracking=detail`） |
| 分析 hprof | MAT（Dominator Tree / Leak Suspects / Path to GC Roots）、JProfiler、VisualVM |

Windows 下 `head` 不存在，用 `... | Select-Object -First 20`（PowerShell）。

---

## 5. 面试话术（STAR）

- **S（情境）**：线上博客服务运行约 6 小时后接口越来越慢，最终 `OutOfMemoryError: Java heap space` 重启，当时没有内存监控面板，只有 GC 日志。
- **T（任务）**：我负责定位根因，并给出既止血又防复发的方案。
- **A（行动）**：
  1. `jstat -gcutil` 判断是堆泄漏而非流量突增（老年代 Full GC 后回收不掉）；
  2. `jmap -histo:live` 看到大量 `byte[]` 和 `ConcurrentHashMap$Node`；
  3. `jmap -dump` + MAT Dominator Tree 锁定 `LocalCacheService.cache`，Path to GC Roots 确认引用链挂在 Spring 单例 Bean 上、永不释放；
  4. 定位到一处"缓存优化"用了永不过期 + 时间戳 key，写入只增不减；
  5. 换成带 TTL + 容量上限的 LRU（生产上我选了 Caffeine），加老年代告警。
- **R（结果）**：修复后运行两周，内存曲线平稳，Full GC 频率从每分钟多次降到偶发。

---

## 6. 高频追问 & 标准答案

- **为什么不用 Redis，非要用本地缓存？** 当时为省一次网络往返做本地缓存扛热点读，低估了 key 基数，容量评估失误。教训：本地缓存必须有容量上限与淘汰。
- **SoftReference / WeakReference 能不能救？** 能缓解不治本；SoftReference 回收时机由 JVM 按内存压力决定、不可控，正确做法是**有界缓存 + 显式淘汰**。
- **怎么区分内存泄漏 vs 内存溢出（大对象一次性打爆）？** 看 OOM dump 的对象分布：泄漏是同类小对象海量堆积且 GC Root 强可达；溢出是单个/少数超大对象。两者 Retained Heap 形态完全不同。
- **堆内 vs 堆外 OOM 排查差异？** 堆内看 heap dump；堆外（Direct）堆使用率正常、进程 RSS 高，靠 NMT / `jcmd VM.native_memory`，且不受 `-Xmx` 约束（本项目 direct 故障演示这点）。
- **GC 死亡螺旋是什么？** 老年代几乎填满 → 每次 Full GC 回收极少 → 停顿越来越长、GC 时间占比逼近 100% → 吞吐趋零 → OOM。jstat 的 FGC/FGCT 快速增长就是它。
- **G1 / CMS 下表现差异？** 可顺势展示 GC 知识（省略，按你准备深度发挥）。

---

## 7. 堆外（Direct）OOM 排查 SOP

> 适用症状：`java.lang.OutOfMemoryError: Direct buffer memory`，或「堆监控正常但进程 RSS 持续膨胀 / 容器被 OOMKilled」。
> 演练复现：配置中心切 `堆外泄漏(direct)` + 开启演练（需 run.bat 的 `-XX:MaxDirectMemorySize=64m`）。

### Step 0 看报错定方向（30 秒）

OOM 报错文案就是分流表，先看清楚是哪一种：

| 报错 | 方向 |
|---|---|
| `Java heap space` | 堆内（走第 2 章流程） |
| `Direct buffer memory` | **堆外直接内存（本章）** |
| `Metaspace` | 类加载泄漏（动态代理/热部署） |
| `unable to create new native thread` | 线程数爆炸，查线程泄漏 |
| 无异常直接消失（`dmesg` 见 OOM Killer / K8s `OOMKilled`） | RSS 碰顶容器限额，堆内堆外都要查 |

### Step 1 确认「堆没事、堆外出事」（1 分钟）

核心判据：**堆使用率正常，但 RSS ≫ 堆上限**。

```bash
jcmd <pid> GC.heap_info          # 堆各区占用，预期：正常、O 区不满
top -p <pid>                     # 看 RES 列（RSS），预期：远超 -Xmx
cat /sys/fs/cgroup/memory.current   # 容器内看 cgroup 实际用量
```

本项目对照：配置中心页面「堆内存」正常、「堆外(direct)」随演练阶梯上涨（口径：`BufferPoolMXBean("direct")`）。

> 为什么堆 dump 分析（jmap -histo）在这里会失效：DirectByteBuffer 的 native 内存不在堆里，
> 堆里只有几十个字节的「壳」，所以堆直方图按 Retained Heap 排序根本排不进前列。

### Step 2 堆外内部分流：direct 还是其他 native？

```bash
# 方式 A：NMT（需启动参数 -XX:NativeMemoryTracking=summary，生产建议常开，开销 <5%）
jcmd <pid> VM.native_memory summary
#   看哪段涨：Internal/Other 涨 → direct buffer / mmap；Thread 涨 → 线程栈；Class 涨 → 元空间

# 方式 B：无 NMT 参数时，直接看 direct buffer 池（JMX/Arthas）
#   Arthas: dashboard 或 memory 命令，直接展示 direct/max 两列
#   JMX: java.lang:type=BufferPool,name=direct → MemoryUsed / TotalCapacity
```

判据：`BufferPool(direct).MemoryUsed ≈ MaxDirectMemorySize` → 坐实 direct 泄漏。

### Step 3 找到「壳对象」的持有者（关键一步）

**堆外泄漏的元凶要靠堆内的「壳」来指认**：每个未释放的 DirectByteBuffer 在堆里都有一个壳实例，壳活着 = native 内存没还。

```bash
jmap -histo:live <pid> | grep DirectByteBuffer
#   实例数 ≈ 泄漏 buffer 数（本项目演练：1 个壳 = 1MB 堆外），正常应用只有零星几个

jmap -dump:format=b,file=./logs/heap_<pid>.hprof <pid>
```

MAT 分析（和堆内流程同套路，只是主角换成 DirectByteBuffer）：
1. Histogram → 搜 `java.nio.DirectByteBuffer` → List objects with GC roots；
2. **Path to GC Roots（exclude 虚/弱引用）**：注意别被 `Cleaner` 的 PhantomReference 干扰，它就是堆外回收机制本身；
3. 引用链会落到「把 buffer 存进静态集合/单例字段」的业务代码。

本项目对照：引用链 = `LeakSimulator（单例 Bean）→ CopyOnWriteArrayList directBufferPool → DirectByteBuffer[]`。

### Step 4 回到代码找分配点

全局 grep 三类堆外分配入口：
- `allocateDirect` / `newDirectByteBuffer`
- `FileChannel.map`（mmap，**不受 MaxDirectMemorySize 约束**，NMT 里在 Internal，且回收更玄学）
- Netty `ByteBuf` / `Unsafe`（引用计数漏 release）

检查模式：分配后是否被长生命周期集合持有？是否该 release/clean 却没调？

### Step 5 止血

| 情况 | 手段 | 效果 |
|---|---|---|
| buffer 已无强引用，只是 Cleaner 没跑 | `jcmd <pid> GC.run`（或等 JVM 自己的慢路径 System.gc()） | 能降，不用重启 |
| 被静态/单例强引用（真泄漏） | **只能重启**（滚动逐台，顺带留一台 dump 取证） | 重启即恢复 |
| 容器反复 OOMKilled | 临时调大 limit 或调小 `-XX:MaxDirectMemorySize` 兑冲 | 买时间用 |

> JVM 内置彩蛋：`Bits.reserveMemory()` 分配失败时会先强制 `System.gc()` 重试一次再抱 OOM——
> 所以 GC 日志里 direct OOM 前会密集出现 `GC cause 'System.gc()'`，看到这个特征基本可以预判堆外要爆。

本项目对照：配置中心「↺ 回收泄漏内存（堆内+堆外）」= 清引用池 + System.gc()，页面「堆外(direct)」当场归零。

### Step 6 修复与防复发

1. 代码：引用封顶（本项目修复态：池超 32 丢最老引用）/ 用完即弃 / Netty 用 try-finally release；
2. 参数：**显式设置 `-XX:MaxDirectMemorySize`**（默认≈Xmx，容器里极易被 OOMKiller 先杀）；
3. 监控：`RSS - 堆上限` 差值告警；Micrometer 自带 `jvm_buffer_memory_used_bytes{id="direct"}` 指标；
4. 取证：Netty 加 `-Dio.netty.leakDetection.level=PARANOID` 可打印未 release 的分配栈；
5. 常开 `-XX:NativeMemoryTracking=summary`，否则事发时没历史可比。

### SOP 一页纸总结（面试背诵版）

> 报错分流 → 堆正常但 RSS 高确认堆外 → NMT/BufferPool 定位 direct → 堆内抓 DirectByteBuffer 壳对象、MAT 追 GC Roots 找持有者 → grep 分配点回代码 → 强引用泄漏只能重启止血 → 修复（封顶引用/显式 MaxDirectMemorySize/RSS 告警/Netty 泄漏检测）。堆外泄漏的精髓：**看堆是看不到的，要靠「壳」和 RSS 的差值破案。**

---

## 8. 本文件涉及的文件清单

| 文件 | 角色 |
|---|---|
| `entity/SysConfig.java` / `mapper/SysConfigMapper.java` | 配置 k-v 存储 |
| `service/SysConfigService.java` | 读写开关（直连 DB，不走被泄漏的 LocalCacheService） |
| `controller/SysConfigController.java` | `/api/admin/config` 配置中心接口（仅 ADMIN） |
| `config/LeakSimulator.java` | 定时轮询开关，注入 heap/direct 故障，支持 fixed 修复态 |
| `config/BoundedCacheService.java` | 修复版有界 LRU 缓存 |
| `config/DataInitializer.java` | 播种默认配置（全关） |
| `db/schema.sql` | `blog_sys_config` 建表 |
| `templates/config.html` + `static/js/app.js` | 配置中心页面与 admin 入口 |
| `run.bat` | 演练 JVM 参数 |
| `application.properties` | `fault.drill.bounded-cache-max-size` |
