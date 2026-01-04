# 微内核/插件化架构指南

## 概述

本监控系统采用**微内核架构（Microkernel Architecture）**，也称为**插件化架构（Plugin Architecture）**。这是一种核心系统与扩展功能分离的架构模式，具有高度的可扩展性和灵活性。

### 架构特点

- **核心简洁** - 核心系统职责单一明确
- **插件独立** - 各插件相互独立，可独立开发和部署
- **动态扩展** - 无需修改核心代码即可添加新功能
- **配置驱动** - 通过配置文件动态组装功能
- **松耦合** - 组件间通过接口通信

---

## 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                     应用入口层                               │
│                  MonitorApplication                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    微内核（核心层）                          │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         MetricCollectorService                       │  │
│  │  - 配置查询：通过ID查询监控元数据                    │  │
│  │  - 任务编排：调度收集模板执行                        │  │
│  │  - 异常处理：统一的错误管理                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐ │
│  │         CollectorTemplate（工作流编排）                │ │
│  │  - 通道获取 → 连接建立 → 命令执行                    │ │
│  │  - 数据解析 → 结果持久化                              │ │
│  └────────────────────────┬───────────────────────────────┘ │
│                           │                                  │
└───────────────────────────┼──────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
   ┌─────────┐         ┌─────────┐        ┌─────────┐
   │ 通道插件  │         │计算器插件 │        │仓储插件  │
   ├─────────┤         ├─────────┤        ├─────────┤
   │ SSH     │         │TopCpu   │        │ Log     │
   │ HTTP    │         │Memory   │        │ MySQL   │
   │ Telnet  │         │Disk     │        │ Redis   │
   └─────────┘         └─────────┘        └─────────┘
```

---

## 代码结构详解

### 1. 项目目录结构

```
monitor/src/main/java/com/tanggo/fund/monitor/
├── MonitorApplication.java                    ← 应用入口
├── core/                                      ← 核心系统
│   ├── entity/                               ← 核心领域对象
│   │   ├── Metric.java                       ← 指标数据
│   │   ├── MetricRetrievalChannel.java       ← 通道接口
│   │   ├── MetricCalculator.java             ← 计算器接口
│   │   └── meta/                             ← 元数据定义
│   │       ├── ChannelMeta.java              ← 通道元数据
│   │       ├── CommandMeta.java              ← 命令元数据
│   │       ├── MetricCalculatorMeta.java     ← 计算器元数据
│   │       └── MetricRetrievalMeta.java      ← 完整监控配置
│   ├── extension/                            ← 扩展点接口
│   │   ├── collector/
│   │   │   ├── MetricRetrievalChannelRepo.java   ← 通道仓储接口
│   │   │   ├── MetricCalculatorRepo.java         ← 计算器仓储接口
│   │   │   ├── MetricPersistRepo.java            ← 持久化仓储接口
│   │   │   └── MetricRetrievalMetaRepo.java      ← 元数据仓储接口
│   │   └── warningrule/                     ← 告警规则扩展点
│   └── service/                              ← 核心服务
│       ├── MetricCollectorService.java       ← 收集服务（微内核）
│       ├── CollectorTemplate.java            ← 收集模板（工作流）
│       ├── MetricQueryService.java           ← 查询服务
│       └── AlertRuleService.java             ← 告警服务
└── plugin/                                   ← 插件实现
    ├── channel/                             ← 通道插件
    │   ├── SSHMetricRetrievalChannel.java   ← SSH实现 ✅
    │   ├── RestMetricRetrievalChannel.java  ← REST实现
    │   └── FTPMetricRetrievalChannel.java   ← FTP实现
    ├── calculator/                          ← 计算器插件
    │   ├── TopCpuMetricCalculator.java      ← CPU解析器 ✅
    │   └── FreeMemoryMetricCalculator.java  ← 内存解析器 ✅
    └── repo/                                ← 仓储插件
        ├── InMemoryMetricRetrievalChannelRepo.java    ← 内存通道仓储
        ├── InMemoryMetricCalculatorRepo.java         ← 内存计算器仓储
        ├── InMemoryMetricRetrievalMetaRepo.java      ← 内存元数据仓储
        ├── LogMetricPersistRepo.java                 ← 日志持久化 ✅
        └── MysqlMetricPersistRepo.java               ← MySQL持久化 ✅
```

### 2. 核心类详解

#### 2.1 Metric（指标数据）

```java
@Data
public class Metric {
    private String meterId;           // 指标ID
    private LocalDateTime timestamp;   // 采集时间
    private String metricName;        // 指标名称
    private double metricValue;       // 指标值
    private Map<String, String> tags; // 标签（用于分类）
}
```

**作用**：携带采集的数据，流转于整个系统

**示例**：
```
metricName: "cpu_usage_percent"
metricValue: 45.3
timestamp: 2026-01-04 10:30:00
tags: {host: "192.168.1.100", service: "api-server"}
```

#### 2.2 接口定义（微内核的扩展点）

**MetricRetrievalChannel 通道接口**
```java
public interface MetricRetrievalChannel {
    void connect(ChannelMeta channelMeta);  // 连接通道
    String execute(CommandMeta cmd);       // 执行命令
}
```
- 定义：输入元数据 → 执行命令 → 返回输出
- 实现：SSH、HTTP、FTP等

**MetricCalculator 计算器接口**
```java
public interface MetricCalculator {
    Metric calculat(String content);  // 解析内容生成指标
}
```
- 定义：输入命令输出文本 → 解析 → 返回Metric
- 实现：CPU解析器、内存解析器等

**MetricPersistRepo 持久化接口**
```java
public interface MetricPersistRepo {
    void insert(Metric metric);  // 持久化指标
}
```
- 定义：输入Metric → 存储 → 返回
- 实现：日志、MySQL、Redis等

#### 2.3 元数据类（配置承载体）

**ChannelMeta 通道元数据**
```java
@Data
public class ChannelMeta {
    private String channelId;              // 通道ID
    private String channelVersion;         // 版本
    private Map<String, Object> extensions;// 扩展配置
}
```
例：SSH连接信息
```json
{
  "channelId": "ssh_cpu_channel",
  "extensions": {
    "host": "192.168.1.100",
    "port": 22,
    "username": "admin",
    "password": "password"
  }
}
```

**CommandMeta 命令元数据**
```java
@Data
public class CommandMeta {
    private String command;                // 执行命令
    private Map<String, Object> extensions;// 扩展配置
}
```
例：CPU监控命令
```json
{
  "command": "top -bn1 -d 1",
  "extensions": {
    "timeout": 30,
    "charset": "UTF-8"
  }
}
```

**MetricRetrievalMeta 完整监控配置**
```java
@Data
public class MetricRetrievalMeta {
    private String monitorId;                      // 监控ID
    private ChannelMeta channelMeta;               // 通道配置
    private CommandMeta commandMeta;               // 命令配置
    private MetricCalculatorMeta metricCalculatorMeta; // 计算器配置
}
```
组合上面三个，形成完整的监控任务配置

#### 2.4 仓储接口（数据访问层）

**MetricRetrievalChannelRepo**
```java
public interface MetricRetrievalChannelRepo {
    MetricRetrievalChannel queryByChannelId(String channelId);
}
```
作用：根据ID查询通道实现

**MetricCalculatorRepo**
```java
public interface MetricCalculatorRepo {
    MetricCalculator queryById(String calculatorId);
}
```
作用：根据ID查询计算器实现

**MetricRetrievalMetaRepo**
```java
public interface MetricRetrievalMetaRepo {
    MetricRetrievalMeta queryById(String monitorId);
}
```
作用：根据ID查询监控配置

**内存实现示例 InMemoryMetricRetrievalMetaRepo**
```java
@AllArgsConstructor
public class InMemoryMetricRetrievalMetaRepo implements MetricRetrievalMetaRepo {
    private final Map<String, MetricRetrievalMeta> metaData;

    @Override
    public MetricRetrievalMeta queryById(String monitorId) {
        return metaData.get(monitorId);  // 简单Map查询
    }
}
```

### 3. 微内核层（MetricCollectorService）

```java
@Slf4j
public class MetricCollectorService {

    @Setter
    private CollectorTemplate collectorTemplate;

    @Setter
    private MetricRetrievalMetaRepo metricRetrievalMetaRepo;

    // 核心方法：通用处理器
    public void handle(String monitorId) {
        log.info("开始处理监控任务: {}", monitorId);
        try {
            // 第1步：查询配置
            MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById(monitorId);
            if (meta == null) {
                log.error("找不到监控配置: {}", monitorId);
                return;
            }

            // 第2步：任务编排
            collectorTemplate.retrieval(meta);

            log.info("监控任务完成: {}", monitorId);
        } catch (Exception e) {
            log.error("监控任务执行失败: {}", monitorId, e);
        }
    }

    // 便捷方法：CPU监控
    public void handleSshCpuMonitor() {
        handle("ssh_cpu_monitor");
    }

    // 便捷方法：内存监控
    public void handleSshMemoryMonitor() {
        handle("ssh_memory_monitor");
    }

    // 便捷方法：执行全部
    public void handleAllMonitors() {
        handleSshCpuMonitor();
        handleSshMemoryMonitor();
    }
}
```

**关键特点**：
- 只有76行代码
- 职责单一：查询 + 编排 + 异常处理
- 不直接依赖任何插件实现
- 所有配置都来自注入，无硬编码

### 4. 模板层（CollectorTemplate）

```java
@Slf4j
public class CollectorTemplate implements Collector {

    @Setter private MetricRetrievalChannelRepo monitorChannelRepo;
    @Setter private MetricCalculatorRepo metricCalculatorRepo;
    @Setter private MetricPersistRepo metricPersistRepo;

    @Override
    public void retrieval(MetricRetrievalMeta esbMetricRetrievalMeta) {

        log.info("开始指标检索流程");

        try {
            // 步骤1：获取通道
            MetricRetrievalChannel channel = monitorChannelRepo.queryByChannelId(
                esbMetricRetrievalMeta.getChannelMeta().getChannelId()
            );

            // 步骤2：建立连接
            channel.connect(esbMetricRetrievalMeta.getChannelMeta());

            // 步骤3：执行命令
            String content = channel.execute(esbMetricRetrievalMeta.getCommandMeta());

            // 步骤4：获取计算器
            MetricCalculator calculator = metricCalculatorRepo.queryById(
                esbMetricRetrievalMeta.getMetricCalculatorMeta().getCalculatorId()
            );

            // 步骤5：数据解析
            Metric metric = calculator.calculat(content);

            // 步骤6：数据持久化
            metricPersistRepo.insert(metric);

            log.info("指标检索流程完成");
        } catch (Exception e) {
            log.error("指标检索流程异常", e);
            throw new RuntimeException("指标检索失败", e);
        }
    }
}
```

**六步工作流**：
1. 查询通道 → 获取具体实现
2. 建立连接 → 初始化通道
3. 执行命令 → 远程执行获得输出
4. 查询计算器 → 获取解析实现
5. 数据解析 → 文本转Metric对象
6. 数据持久化 → 保存数据

### 5. 插件层（以SSH为例）

**SSHMetricRetrievalChannel**
```java
@Slf4j
public class SSHMetricRetrievalChannel implements MetricRetrievalChannel {

    private Session session;
    private String host, username, password;
    private int port;

    @Override
    public void connect(ChannelMeta channelMeta) {
        // 从元数据中提取SSH连接信息
        Map<String, Object> ext = channelMeta.getExtensions();
        this.host = (String) ext.get("host");
        this.port = ((Number) ext.getOrDefault("port", 22)).intValue();
        this.username = (String) ext.get("username");
        this.password = (String) ext.get("password");

        // 建立JSch连接
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);

        log.info("SSH连接成功: {}@{}:{}", username, host, port);
    }

    @Override
    public String execute(CommandMeta cmd) {
        // 获取命令和超时参数
        String command = cmd.getCommand();
        int timeout = ((Number) cmd.getExtensions().getOrDefault("timeout", 30)).intValue();

        try {
            // 打开通道执行命令
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(channel.getInputStream())
            );

            channel.connect();

            // 读取输出
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            channel.disconnect();

            log.info("命令执行成功: {}", command);
            return output.toString();
        } catch (Exception e) {
            log.error("命令执行失败: {}", command, e);
            throw new RuntimeException(e);
        }
    }
}
```

### 6. 数据流示例

执行过程完整数据流：

```
用户调用
  │
  ├─ service.handleSshCpuMonitor()
  │
  ▼
MetricCollectorService.handle("ssh_cpu_monitor")
  │
  ├─ queryById("ssh_cpu_monitor")
  │
  ▼
MetricRetrievalMeta {
  channelMeta: {
    channelId: "ssh_cpu_channel",
    extensions: {host: "192.168.1.100", port: 22, ...}
  },
  commandMeta: {
    command: "top -bn1 -d 1"
  },
  calculatorMeta: {
    calculatorId: "top_cpu_calculator"
  }
}
  │
  ├─ collectorTemplate.retrieval(meta)
  │
  ▼
CollectorTemplate {
  // 步骤1：获取SSH通道
  channel = monitorChannelRepo.queryByChannelId("ssh_cpu_channel")
           → SSHMetricRetrievalChannel实例

  // 步骤2：连接
  channel.connect(channelMeta)
         → 建立SSH连接

  // 步骤3：执行命令
  output = channel.execute(commandMeta)
          → "top -bn1 -d 1" 命令输出

  // 步骤4：获取计算器
  calculator = metricCalculatorRepo.queryById("top_cpu_calculator")
             → TopCpuMetricCalculator实例

  // 步骤5：解析数据
  metric = calculator.calculat(output)
          → Metric {
              metricName: "cpu_usage_percent",
              metricValue: 45.3,
              timestamp: 2026-01-04 10:30:00,
              tags: {host: "192.168.1.100"}
            }

  // 步骤6：持久化
  metricPersistRepo.insert(metric)
                   → 保存到日志或MySQL
}
  │
  ▼
完成
```

### 7. Spring XML配置体系

**spring-ssh-cpu-monitor.xml** (主配置)
- 导入元数据配置
- 导入组件配置
- 声明仓储Bean
- 声明服务Bean

**spring-meta-config.xml** (元数据配置)
- 定义SSH连接参数
- 定义命令参数
- 定义计算器ID
- 组合成完整监控配置

**spring-component-config.xml** (组件配置)
- 声明SSHMetricRetrievalChannel Bean
- 声明TopCpuMetricCalculator Bean
- 声明FreeMemoryMetricCalculator Bean

**spring-mysql-config.xml** (数据库配置)
- 声明数据源
- 声明MySQL仓储

---

## 核心概念

### 1. 微内核（Kernel Core）

**作用**：系统的最小集合，提供基础功能和扩展点

**MetricCollectorService 的微内核职责**：

```java
public class MetricCollectorService {
    // 核心职责 #1：依赖注入（通过Setter）
    @Setter
    private CollectorTemplate collectorTemplate;

    @Setter
    private MetricRetrievalMetaRepo metricRetrievalMetaRepo;

    // 核心职责 #2：配置查询
    public void handle(String monitorId) {
        MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById(monitorId);
        if (meta == null) {
            log.error("找不到监控配置: {}", monitorId);
            return;
        }
    }

    // 核心职责 #3：任务编排
    collectorTemplate.retrieval(meta);
}
```

**核心的三个职责**：
1. **配置注册** - 接收从Spring XML配置的依赖
2. **配置查询** - 根据ID查询监控元数据配置
3. **任务编排** - 将配置转交给模板执行

### 2. 插件（Plugins）

**插件类型**：

#### 类型A - 通道插件（Channel Plugin）

**作用**：实现数据采集的传输通道

```
接口定义（核心）：
┌────────────────────────────┐
│ MetricRetrievalChannel     │
├────────────────────────────┤
│ + connect(ChannelMeta)     │
│ + execute(CommandMeta)     │
│ + disconnect()             │
└────────────────────────────┘
         △
         │ implements
    ┌────┴────┬──────────┐
    │          │          │
SSHChannel  HTTPChannel  TelnetChannel
(已实现)    (可扩展)     (可扩展)
```

**现有实现**：
- ✅ **SSHMetricRetrievalChannel** - SSH远程执行
- 📋 **HTTPChannel** - 可扩展：HTTP API调用
- 📋 **TelnetChannel** - 可扩展：Telnet协议

**插件特性**：
```xml
<!-- spring-component-config.xml -->
<bean id="sshMetricRetrievalChannel"
      class="com.tanggo.fund.monitor.plugin.channel.SSHMetricRetrievalChannel"/>

<!-- 添加新通道只需：
     1. 实现MetricRetrievalChannel接口
     2. 在XML中声明新Bean
     3. 在仓储中注册 -->
```

#### 类型B - 计算器插件（Calculator Plugin）

**作用**：实现数据解析和指标计算

```
接口定义（核心）：
┌────────────────────────────┐
│ MetricCalculator           │
├────────────────────────────┤
│ + calculate(String output) │
│   : Metric                 │
└────────────────────────────┘
         △
         │ implements
    ┌────┴────┬──────────┐
    │          │          │
TopCpuCalc  MemoryCalc  DiskCalc
(已实现)    (已实现)    (可扩展)
```

**现有实现**：
- ✅ **TopCpuMetricCalculator** - 解析top命令输出
- ✅ **FreeMemoryMetricCalculator** - 解析free命令输出
- 📋 **DiskCalculator** - 可扩展：df命令解析
- 📋 **NetworkCalculator** - 可扩展：netstat命令解析

**插件特性**：
```java
// 添加新计算器只需实现接口
@Slf4j
public class DiskMetricCalculator implements MetricCalculator {
    @Override
    public Metric calculate(String output) {
        // 解析 df -h 输出
        // 提取磁盘使用率、容量等指标
        return new Metric(...);
    }
}
```

#### 类型C - 仓储插件（Repository Plugin）

**作用**：实现数据持久化的不同后端

```
接口定义（核心）：
┌────────────────────────────┐
│ MetricPersistRepo          │
├────────────────────────────┤
│ + insert(Metric)           │
└────────────────────────────┘
         △
         │ implements
    ┌────┴────┬──────────┐
    │          │          │
LogRepo    MySQLRepo   RedisRepo
(已实现)   (已实现)    (可扩展)
```

**现有实现**：
- ✅ **LogMetricPersistRepo** - 日志输出（开发环境）
- ✅ **MysqlMetricPersistRepo** - MySQL数据库（生产环境）
- 📋 **RedisMetricPersistRepo** - 可扩展：Redis缓存
- 📋 **InfluxDBMetricPersistRepo** - 可扩展：时序数据库
- 📋 **ClickHouseMetricPersistRepo** - 可扩展：OLAP数据库

**切换插件的方式**（仅需修改XML）：
```xml
<!-- 开发环境：使用日志输出 -->
<bean id="metricPersistRepo"
      class="com.tanggo.fund.monitor.plugin.repo.LogMetricPersistRepo"/>

<!-- 生产环境：切换为MySQL -->
<bean id="metricPersistRepo"
      class="com.tanggo.fund.monitor.plugin.repo.MysqlMetricPersistRepo">
    <constructor-arg ref="dataSource"/>
</bean>

<!-- 高性能环境：切换为Redis -->
<bean id="metricPersistRepo"
      class="com.tanggo.fund.monitor.plugin.repo.RedisMetricPersistRepo">
    <constructor-arg ref="redisTemplate"/>
</bean>
```

---

## 工作流程解析

### MetricCollectorService 的执行流程

```
用户调用                        核心系统处理                          插件执行
      │                             │                                │
      │                             │                                │
      ├─ handleSshCpuMonitor()      │                                │
      │                             ▼                                │
      │                    handle("ssh_cpu_monitor")                 │
      │                             │                                │
      │                             ├─ 查询配置                       │
      │                             │  metricRetrievalMetaRepo       │
      │                             │  .queryById(monitorId)         │
      │                             │                                │
      │                             │  返回：MetricRetrievalMeta     │
      │                             │  ├─ channelMeta               │
      │                             │  │  ├─ host: 192.168.1.100   │
      │                             │  │  ├─ port: 22               │
      │                             │  │  └─ username/password      │
      │                             │  ├─ commandMeta              │
      │                             │  │  └─ command: top -bn1 -d 1│
      │                             │  └─ calculatorMeta           │
      │                             │     └─ calculatorId          │
      │                             │                                │
      │                             ├─ 执行收集                       │
      │                             │  collectorTemplate.retrieval()│
      │                             │                                │
      │                             ├─────────────────────────────┬──┼─────────┐
      │                             │                             │  │         │
      │                             │ 1. 通道插件执行              │  │         │
      │                             ├──────────────────────────────┼──▶ SSH连接
      │                             │                             │  │ │
      │                             │ 2. 命令执行                  │  │ └─▶top -bn1 -d 1
      │                             │                             │  │    │
      │                             │ 3. 获取计算器                │  │    ▼
      │                             │                             │  │    输出解析
      │                             │ 4. 数据计算                  │  │    │
      │                             ├──────────────────────────────┼──▶ TopCpuCalculator
      │                             │                             │  │ ├─ cpu_usage
      │                             │ 5. 数据持久化                │  │ ├─ memory_used
      │                             ├──────────────────────────────┼──▶ MetricPersistRepo
      │                             │                             │  │ └─ 保存到MySQL/Log
      │                             │                             │  │
      │◀────────────────────────────┴─────────────────────────────┴──┘
      │
   完成
```

### 代码执行的关键位置

**位置1：查询配置（第32行）**
```java
MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById(monitorId);
```
- 从仓储中查询配置
- 返回完整的元数据对象

**位置2：任务编排（第42行）**
```java
collectorTemplate.retrieval(meta);
```
- 将配置传给模板
- 模板负责具体的执行流程

**位置3：异常处理（第45-46行）**
```java
catch (Exception e) {
    log.error("监控任务执行失败: {}", monitorId, e);
}
```
- 统一的错误处理
- 防止一个任务失败影响其他任务

---

## 配置驱动的插件管理

### 1. 配置层次结构

```
spring-ssh-cpu-monitor.xml (主配置)
├── <import resource="spring-meta-config.xml"/>
│   └─ 元数据配置
│      ├─ sshCpuMetricRetrievalMeta (监控#1)
│      └─ sshMemoryMetricRetrievalMeta (监控#2)
├── <import resource="spring-component-config.xml"/>
│   └─ 插件实现
│      ├─ sshMetricRetrievalChannel
│      ├─ topCpuMetricCalculator
│      └─ freeMemoryMetricCalculator
└─ 服务层配置
   ├─ metricRetrievalMetaRepo
   │  └─ 注册所有元数据
   ├─ metricCalculatorRepo
   │  └─ 注册所有计算器
   ├─ metricRetrievalChannelRepo
   │  └─ 注册所有通道
   └─ MetricCollectorService
      └─ 注入上述所有仓储
```

### 2. 注册机制

**通过Map注册插件**：

```xml
<!-- 通道插件注册 -->
<bean id="metricRetrievalChannelRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricRetrievalChannelRepo">
    <constructor-arg>
        <map>
            <entry key="ssh_cpu_channel" value-ref="sshMetricRetrievalChannel"/>
            <!-- 可扩展添加更多通道
            <entry key="http_channel" value-ref="httpMetricRetrievalChannel"/>
            <entry key="telnet_channel" value-ref="telnetMetricRetrievalChannel"/>
            -->
        </map>
    </constructor-arg>
</bean>

<!-- 计算器插件注册 -->
<bean id="metricCalculatorRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricCalculatorRepo">
    <constructor-arg>
        <map>
            <entry key="top_cpu_calculator" value-ref="topCpuMetricCalculator"/>
            <entry key="memory_calculator" value-ref="freeMemoryMetricCalculator"/>
            <!-- 可扩展添加更多计算器
            <entry key="disk_calculator" value-ref="diskMetricCalculator"/>
            <entry key="network_calculator" value-ref="networkMetricCalculator"/>
            -->
        </map>
    </constructor-arg>
</bean>
```

### 3. 监控元数据注册

```xml
<!-- 监控配置注册 -->
<bean id="metricRetrievalMetaRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricRetrievalMetaRepo">
    <constructor-arg>
        <map>
            <!-- 监控#1: SSH CPU -->
            <entry key="ssh_cpu_monitor" value-ref="sshCpuMetricRetrievalMeta"/>

            <!-- 监控#2: SSH 内存 -->
            <entry key="ssh_memory_monitor" value-ref="sshMemoryMetricRetrievalMeta"/>

            <!-- 可扩展添加更多监控任务
            <entry key="ssh_disk_monitor" value-ref="sshDiskMetricRetrievalMeta"/>
            <entry key="http_api_monitor" value-ref="httpApiMetricRetrievalMeta"/>
            -->
        </map>
    </constructor-arg>
</bean>
```

---

## 扩展指南

### 场景1：添加新的通道插件

**需求**：添加HTTP通道支持从HTTP API获取指标

**步骤**：

**1️⃣ 创建插件类**
```java
// src/main/java/com/tanggo/fund/monitor/plugin/channel/HTTPMetricRetrievalChannel.java
@Slf4j
public class HTTPMetricRetrievalChannel implements MetricRetrievalChannel {
    private String baseUrl;
    private int timeout;

    @Override
    public void connect(ChannelMeta channelMeta) {
        Map<String, Object> ext = channelMeta.getExtensions();
        this.baseUrl = (String) ext.get("baseUrl");
        this.timeout = ((Number) ext.getOrDefault("timeout", 30000)).intValue();
        log.info("HTTP通道已连接: {}", baseUrl);
    }

    @Override
    public String execute(CommandMeta cmd) {
        // 例如：cmd.getCommand() = "/api/metrics/cpu"
        String endpoint = cmd.getCommand();
        try {
            String fullUrl = baseUrl + endpoint;
            // 使用HttpClient调用API
            return httpClient.get(fullUrl, timeout);
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", fullUrl, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect() {
        log.info("HTTP通道已断开");
    }
}
```

**2️⃣ 在spring-component-config.xml中声明**
```xml
<!-- 添加到 spring-component-config.xml -->
<bean id="httpMetricRetrievalChannel"
      class="com.tanggo.fund.monitor.plugin.channel.HTTPMetricRetrievalChannel"/>
```

**3️⃣ 在spring-ssh-cpu-monitor.xml中注册**
```xml
<!-- 修改 metricRetrievalChannelRepo -->
<bean id="metricRetrievalChannelRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricRetrievalChannelRepo">
    <constructor-arg>
        <map>
            <entry key="ssh_cpu_channel" value-ref="sshMetricRetrievalChannel"/>
            <entry key="http_channel" value-ref="httpMetricRetrievalChannel"/>  <!-- 新增 -->
        </map>
    </constructor-arg>
</bean>
```

**4️⃣ 在spring-meta-config.xml中创建HTTP监控元数据**
```xml
<!-- HTTP通道元数据 -->
<bean id="httpChannelMeta" class="com.tanggo.fund.monitor.core.entity.meta.ChannelMeta">
    <property name="channelId" value="http_channel"/>
    <property name="channelVersion" value="1.0"/>
    <property name="extensions">
        <map>
            <entry key="baseUrl" value="http://api.example.com"/>
            <entry key="timeout" value="30000"/>
        </map>
    </property>
</bean>

<!-- CPU API元数据 -->
<bean id="cpuApiCommandMeta" class="com.tanggo.fund.monitor.core.entity.meta.CommandMeta">
    <property name="command" value="/api/metrics/cpu"/>
    <property name="extensions">
        <map>
            <entry key="timeout" value="10"/>
        </map>
    </property>
</bean>

<!-- HTTP CPU监控完整配置 -->
<bean id="httpCpuMetricRetrievalMeta"
      class="com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta">
    <property name="monitorId" value="http_cpu_monitor"/>
    <property name="channelMeta" ref="httpChannelMeta"/>
    <property name="commandMeta" ref="cpuApiCommandMeta"/>
    <property name="metricCalculatorMeta" ref="topCpuCalculatorMeta"/>
</bean>
```

**5️⃣ 在spring-ssh-cpu-monitor.xml中注册监控**
```xml
<!-- 修改 metricRetrievalMetaRepo -->
<bean id="metricRetrievalMetaRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricRetrievalMetaRepo">
    <constructor-arg>
        <map>
            <entry key="ssh_cpu_monitor" value-ref="sshCpuMetricRetrievalMeta"/>
            <entry key="ssh_memory_monitor" value-ref="sshMemoryMetricRetrievalMeta"/>
            <entry key="http_cpu_monitor" value-ref="httpCpuMetricRetrievalMeta"/>  <!-- 新增 -->
        </map>
    </constructor-arg>
</bean>
```

**6️⃣ 使用新的监控**
```java
// MonitorApplication.java
MetricCollectorService service = context.getBean(MetricCollectorService.class);

// 执行HTTP CPU监控
service.handle("http_cpu_monitor");

// 或者添加便捷方法
// service.handleHttpCpuMonitor();
```

**✅ 完成！** 无需修改MetricCollectorService、CollectorTemplate等核心代码

---

### 场景2：添加新的计算器插件

**需求**：添加磁盘使用率监控

**步骤**：

**1️⃣ 创建计算器类**
```java
// src/main/java/com/tanggo/fund/monitor/plugin/calculator/DiskMetricCalculator.java
@Slf4j
public class DiskMetricCalculator implements MetricCalculator {

    @Override
    public Metric calculate(String output) {
        /*
        df -h 输出格式：
        Filesystem     Size  Used Avail Use% Mounted on
        /dev/sda1       50G   30G   20G  60%  /
        /dev/sda2      100G   80G   20G  80%  /home
        */

        Metric metric = new Metric();
        metric.setMetricName("disk_usage");
        metric.setTimestamp(LocalDateTime.now());

        try {
            String[] lines = output.split("\n");
            double totalUsagePercent = 0;
            int diskCount = 0;

            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].trim().split("\\s+");
                if (parts.length >= 5) {
                    String usePercent = parts[4].replace("%", "");
                    totalUsagePercent += Double.parseDouble(usePercent);
                    diskCount++;
                }
            }

            if (diskCount > 0) {
                double avgUsage = totalUsagePercent / diskCount;
                metric.setMetricValue(avgUsage);
                metric.setMetricName("disk_usage_percent");
                log.info("磁盘使用率: {}%", avgUsage);
            }
        } catch (Exception e) {
            log.error("磁盘指标解析失败", e);
        }

        return metric;
    }
}
```

**2️⃣ 在spring-component-config.xml中声明**
```xml
<bean id="diskMetricCalculator"
      class="com.tanggo.fund.monitor.plugin.calculator.DiskMetricCalculator"/>
```

**3️⃣ 注册计算器**
```xml
<!-- 修改 metricCalculatorRepo -->
<bean id="metricCalculatorRepo"
      class="com.tanggo.fund.monitor.plugin.repo.InMemoryMetricCalculatorRepo">
    <constructor-arg>
        <map>
            <entry key="top_cpu_calculator" value-ref="topCpuMetricCalculator"/>
            <entry key="memory_calculator" value-ref="freeMemoryMetricCalculator"/>
            <entry key="disk_calculator" value-ref="diskMetricCalculator"/>  <!-- 新增 -->
        </map>
    </constructor-arg>
</bean>
```

**4️⃣ 创建监控元数据**
```xml
<!-- 磁盘命令元数据 -->
<bean id="dfCommandMeta" class="com.tanggo.fund.monitor.core.entity.meta.CommandMeta">
    <property name="command" value="df -h"/>
    <property name="extensions">
        <map>
            <entry key="timeout" value="30"/>
        </map>
    </property>
</bean>

<!-- 磁盘计算器元数据 -->
<bean id="diskCalculatorMeta" class="com.tanggo.fund.monitor.core.entity.meta.MetricCalculatorMeta">
    <property name="calculatorId" value="disk_calculator"/>
</bean>

<!-- SSH磁盘监控配置 -->
<bean id="sshDiskMetricRetrievalMeta"
      class="com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta">
    <property name="monitorId" value="ssh_disk_monitor"/>
    <property name="channelMeta" ref="sshChannelMeta"/>
    <property name="commandMeta" ref="dfCommandMeta"/>
    <property name="metricCalculatorMeta" ref="diskCalculatorMeta"/>
</bean>
```

**5️⃣ 注册监控**
```xml
<!-- 修改 metricRetrievalMetaRepo -->
<entry key="ssh_disk_monitor" value-ref="sshDiskMetricRetrievalMeta"/>
```

**6️⃣ 使用新监控**
```java
service.handle("ssh_disk_monitor");
```

**✅ 完成！** 又添加了一个新的监控能力

---

### 场景3：切换持久化后端

**需求**：从MySQL切换到Redis缓存

**步骤**：

**1️⃣ 创建Redis仓储实现**
```java
// src/main/java/com/tanggo/fund/monitor/plugin/repo/RedisMetricPersistRepo.java
@Slf4j
public class RedisMetricPersistRepo implements MetricPersistRepo {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisMetricPersistRepo(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void insert(Metric metric) {
        try {
            String key = "metric:" + metric.getMetricName() + ":" + metric.getTimestamp();
            String value = new ObjectMapper().writeValueAsString(metric);

            // 1小时过期
            redisTemplate.opsForValue().set(key, value, Duration.ofHours(1));

            log.info("指标已保存到Redis: {} = {}", metric.getMetricName(), metric.getMetricValue());
        } catch (Exception e) {
            log.error("保存指标到Redis异常", e);
        }
    }
}
```

**2️⃣ 修改spring-mysql-config.xml为spring-redis-config.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Redis连接工厂 -->
    <bean id="redisConnectionFactory"
          class="org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory">
        <constructor-arg>
            <bean class="org.springframework.data.redis.connection.RedisStandaloneConfiguration">
                <property name="hostName" value="localhost"/>
                <property name="port" value="6379"/>
            </bean>
        </constructor-arg>
    </bean>

    <!-- RedisTemplate -->
    <bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate">
        <property name="connectionFactory" ref="redisConnectionFactory"/>
    </bean>

    <!-- Redis持久化仓储 -->
    <bean id="redisMetricPersistRepo"
          class="com.tanggo.fund.monitor.plugin.repo.RedisMetricPersistRepo">
        <constructor-arg ref="redisTemplate"/>
    </bean>
</beans>
```

**3️⃣ 在spring-ssh-cpu-monitor.xml中修改配置**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans ...>

    <!-- 导入Redis配置而非MySQL配置 -->
    <import resource="spring-redis-config.xml"/>
    <!-- <import resource="spring-mysql-config.xml"/> -->

    <!-- 使用Redis仓储 -->
    <bean id="metricPersistRepo" class="com.tanggo.fund.monitor.plugin.repo.RedisMetricPersistRepo">
        <constructor-arg ref="redisTemplate"/>
    </bean>

    <!-- ... 其他配置 ... -->
</beans>
```

**✅ 完成！** 无需修改任何Java代码，仅修改XML配置即可切换持久化后端

---

## 微内核架构的优势

| 优势 | 说明 | 示例 |
|------|------|------|
| **易于扩展** | 添加新功能无需修改核心 | 新增HTTP通道、磁盘计算器 |
| **易于维护** | 代码改动局限于插件 | 修改TopCpuCalculator只影响计算器 |
| **易于测试** | 插件可独立单元测试 | 测试DiskCalculator无需SSH连接 |
| **易于部署** | 可灰度部署新插件 | 先在测试环境测试新计算器 |
| **易于理解** | 核心代码简洁 | MetricCollectorService只有76行 |
| **灵活配置** | 通过XML动态组装功能 | 切换持久化无需重新编译 |
| **团队分工** | 不同团队开发不同插件 | 前端团队开发UI插件，数据团队开发计算器 |

---

## 微内核与其他架构的对比

### vs 单体架构（Monolithic）

| 特性 | 微内核 | 单体 |
|------|-------|------|
| 代码耦合 | 低 | 高 |
| 扩展性 | 强 | 弱 |
| 部署灵活性 | 高 | 低 |
| 学习曲线 | 陡 | 平缓 |

### vs 微服务架构（Microservices）

| 特性 | 微内核 | 微服务 |
|------|-------|--------|
| 复杂度 | 低 | 高 |
| 部署成本 | 低 | 高 |
| 可扩展性 | 中 | 高 |
| 运维成本 | 低 | 高 |

---

## 最佳实践

### ✅ 应该做

1. **插件职责单一**
   ```java
   // ✅ 好：只负责CPU数据解析
   public class TopCpuMetricCalculator implements MetricCalculator {
       public Metric calculate(String output) {
           // 仅解析CPU指标
       }
   }
   ```

2. **通过接口通信**
   ```java
   // ✅ 好：依赖抽象接口
   private MetricRetrievalChannel channel;  // 接口
   ```

3. **核心保持简洁**
   ```java
   // ✅ 好：MetricCollectorService只有核心职责
   public void handle(String monitorId) {
       MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById(monitorId);
       collectorTemplate.retrieval(meta);
   }
   ```

4. **配置驱动**
   ```xml
   <!-- ✅ 好：所有组件都在XML中配置 -->
   <entry key="http_channel" value-ref="httpMetricRetrievalChannel"/>
   ```

### ❌ 不应该做

1. **混合职责**
   ```java
   // ❌ 差：计算器混合了SSH连接逻辑
   public class BadCalculator implements MetricCalculator {
       public Metric calculate(String output) {
           // 不应该在这里建立SSH连接
           JSch jsch = new JSch();
       }
   }
   ```

2. **硬编码依赖**
   ```java
   // ❌ 差：直接实例化而非注入
   private MetricRetrievalChannel channel = new SSHMetricRetrievalChannel();
   ```

3. **核心代码耦合**
   ```java
   // ❌ 差：核心代码依赖具体实现
   public void handle(String monitorId) {
       if ("ssh".equals(type)) {
           channel = new SSHChannel();
       } else if ("http".equals(type)) {
           channel = new HTTPChannel();
       }
   }
   ```

4. **绕过接口直接访问**
   ```java
   // ❌ 差：绕过通道接口直接使用JSch
   JSch jsch = new JSch();
   session = jsch.getSession(...);
   ```

---

## 完整扩展清单

| 功能 | 状态 | 实现位置 | 配置位置 |
|------|------|---------|---------|
| SSH通道 | ✅ | plugin/channel/SSHMetricRetrievalChannel.java | spring-component-config.xml |
| HTTP通道 | 📋 | 待实现 | spring-component-config.xml |
| Telnet通道 | 📋 | 待实现 | spring-component-config.xml |
| CPU计算器 | ✅ | plugin/calculator/TopCpuMetricCalculator.java | spring-component-config.xml |
| 内存计算器 | ✅ | plugin/calculator/FreeMemoryMetricCalculator.java | spring-component-config.xml |
| 磁盘计算器 | 📋 | 待实现 | spring-component-config.xml |
| 网络计算器 | 📋 | 待实现 | spring-component-config.xml |
| 日志仓储 | ✅ | plugin/repo/LogMetricPersistRepo.java | spring-ssh-cpu-monitor.xml |
| MySQL仓储 | ✅ | plugin/repo/MysqlMetricPersistRepo.java | spring-mysql-config.xml |
| Redis仓储 | 📋 | 待实现 | spring-redis-config.xml |
| InfluxDB仓储 | 📋 | 待实现 | spring-influxdb-config.xml |

---

## 总结

**MetricCollectorService 展示的微内核架构核心思想**：

```
               配置
                │
                ▼
  ┌──────────────────────────┐
  │  MetricCollectorService  │  ◄─ 微内核：简洁、稳定、不变
  │   (查询 + 编排 + 异常)    │
  └──────────────────────────┘
                │
                ▼
  ┌──────────────────────────┐
  │  CollectorTemplate       │  ◄─ 核心流程：标准化执行
  └──────────────────────────┘
           │    │    │
           ▼    ▼    ▼
        ┌──┬──┬──┐  ◄─ 插件：独立、可扩展、可替换
        插件 插件 插件
```

通过这个设计，系统具备：
- ✅ **高度的可扩展性** - 添加新功能只需添加插件
- ✅ **低耦合性** - 核心不依赖具体实现
- ✅ **易维护性** - 改动局限于插件
- ✅ **高灵活性** - 配置驱动，无需重编译

这正是微内核/插件化架构的精妙之处！
