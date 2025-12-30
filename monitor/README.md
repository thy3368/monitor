# Monitor - 企业级插件化监控系统框架

一个遵循 Clean Architecture 原则的高性能、可扩展的企业级监控系统框架。支持灵活的插件化架构，提供了完整的监控、计算、告警和持久化解决方案。

## 项目概述

Monitor 框架是一个企业级监控解决方案，专注于以下核心能力：

- **Clean Architecture 架构**: 严格遵循整洁架构原则，核心业务逻辑完全独立于框架和外部系统
- **完整的插件化体系**: 支持自定义监控器、通道、计算器、持久化和告警处理实现
- **多通道支持**: 内置 SSH、本地等监控通道，扩展新通道无需修改核心代码
- **灵活的指标系统**: 支持多种指标（Meter）的定义、计算和聚合
- **完善的告警处理**: 集成告警条件判断、动作分发和执行机制
- **低延迟优化**: 遵循低延迟开发标准，支持高频率的监控采样和处理

## 技术栈

- **Java**: 17+
- **Spring Boot**: 4.0.1
- **Build Tool**: Maven 3.6+
- **Architecture Pattern**: Clean Architecture
- **Performance Target**: 关键路径 < 1μs

## 项目结构

```
monitor/
├── pom.xml                              # Maven 项目配置
├── .mvn/                                # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/tanggo/fund/monitor/
│   │   │   ├── core/                    # 核心领域层
│   │   │   │   ├── entity/              # 领域实体
│   │   │   │   │   ├── Monitor.java     # 监控器接口
│   │   │   │   │   ├── MonitorChannel.java    # 监控通道接口
│   │   │   │   │   ├── MeterCalculator.java   # 监控计算器接口
│   │   │   │   │   ├── ActionHandle.java      # 动作处理器接口
│   │   │   │   │   ├── Meter.java      # 指标数据
│   │   │   │   │   ├── Action.java     # 告警动作
│   │   │   │   │   └── meta/           # 元数据定义
│   │   │   │   │       ├── MonitorMeta.java       # 监控器元数据
│   │   │   │   │       ├── ChannelMeta.java       # 通道元数据
│   │   │   │   │       ├── CommandMeta.java       # 命令元数据
│   │   │   │   │       └── MeterCalculatorMeta.java   # 计算器元数据
│   │   │   │   └── repo/                # 仓储接口
│   │   │   │       ├── MonitorRepo.java
│   │   │   │       ├── MonitorChannelRepo.java
│   │   │   │       ├── MeterCalculatorRepo.java
│   │   │   │       ├── MeterPersistRepo.java
│   │   │   │       └── WarningProcessRepo.java
│   │   │   ├── adapter/                 # 接口适配层
│   │   │   │   ├── channel/             # 通道实现
│   │   │   │   │   ├── SSHMonitorChannel.java      # SSH 通道
│   │   │   │   │   ├── FTPMonitorChannel.java      # FTP 通道
│   │   │   │   │   └── RestMonitorChannel.java     # REST 通道
│   │   │   │   ├── template/            # 监控器模板实现
│   │   │   │   │   └── CommonMonitor.java          # 通用监控器
│   │   │   │   ├── monitor/             # 监控器实现
│   │   │   │   │   └── ActionHandleRepo.java       # 动作处理器
│   │   │   │   └── meterpersisrepo/     # 持久化实现
│   │   │   │       └── MysqlMeterPersistRepo.java  # MySQL 持久化
│   │   │   ├── example/                 # 使用示例
│   │   │   │   └── MonitorProcess.java
│   │   │   └── MonitorApplication.java  # 应用入口
│   │   └── resources/
│   │       └── application.properties   # 应用配置
│   └── test/
│       └── java/com/tanggo/fund/monitor/
│           └── MonitorApplicationTests.java  # 测试代码
└── README.md                            # 项目说明文档
```

### 目录说明

| 目录 | 说明 |
|------|------|
| `core/entity` | 领域实体和值对象，包含纯业务逻辑 |
| `core/entity/meta` | 元数据定义，用于配置和参数化监控器 |
| `core/repo` | 仓储接口，定义数据访问契约 |
| `adapter/channel` | 多种通道实现（SSH、FTP、REST 等） |
| `adapter/template` | 监控器模板类（CommonMonitor） |
| `adapter/monitor` | 具体的监控器实现和相关处理器 |
| `adapter/meterpersisrepo` | 数据持久化实现（MySQL 等） |
| `example` | 使用示例代码 |

## 核心概念

### Monitor (监控器)

Monitor 是监控系统的基础单元，负责编排整个监控执行流程。它是领域驱动设计中的聚合根，统一协调各个组件的交互。

```java
public interface Monitor {
    void execution(MonitorMeta esbMonitorMeta);
}
```

#### Monitor 元数据定义

Monitor 通过元数据来描述其配置和执行属性，支持灵活的扩展和动态配置。主要由以下三个核心元数据组成：

##### 1. MonitorMeta (监控器元数据)

```java
@Data
public class MonitorMeta {
    private ChannelMeta channelMeta;           // 通道元数据
    private CommandMeta commandMeta;           // 命令元数据
    private MeterCalculatorMeta meterCalculatorMeta;  // 计算器元数据
}
```

##### 2. ChannelMeta (通道元数据)

```java
@Data
public class ChannelMeta {
    private String channelId;                  // 通道唯一标识
    private String channelVersion;             // 通道版本
    private Map<String, Objects> extensions;   // 扩展配置参数
}
```

**扩展参数示例**:
```java
// SSH 通道扩展配置
extensions.put("host", "192.168.1.100");
extensions.put("port", 22);
extensions.put("username", "admin");
extensions.put("password", "password");
extensions.put("timeout", 10000);

// FTP 通道扩展配置
extensions.put("host", "192.168.1.100");
extensions.put("port", 21);
extensions.put("username", "ftpuser");
extensions.put("password", "password");
```

##### 3. CommandMeta (命令元数据)

```java
@Data
public class CommandMeta {
    private String command;                    // 要执行的命令
    private Map<String, Objects> extensions;   // 扩展配置参数
}
```

**命令示例**:
```java
// 查询系统状态命令
commandMeta.setCommand("query_status");

// 查询 CPU 使用率
commandMeta.setCommand("cat /proc/cpuinfo");

// 查询内存使用
commandMeta.setCommand("free -m");

// 扩展参数可包含
extensions.put("timeout", 5000);
extensions.put("retryCount", 3);
```

##### 4. MeterCalculatorMeta (计算器元数据)

```java
@Data
public class MeterCalculatorMeta {
    private String calculatorId;               // 计算器唯一标识
    private Map<String, Objects> extensions;   // 扩展配置参数
}
```

**扩展参数示例**:
```java
// 性能指标计算器
extensions.put("metricType", "PERFORMANCE");
extensions.put("aggregationWindow", 60000);  // 聚合窗口：60秒

// 日志解析计算器
extensions.put("parsingRule", "pattern1");
extensions.put("filterCondition", "ERROR");
```

#### Monitor 的完整执行流程示例

Monitor 的执行流程通过 `CommonMonitor` 实现类展示：

```java
public class CommonMonitor implements Monitor {

    private MonitorChannelRepo monitorChannelRepo;
    private MeterCalculatorRepo meterCalculatorRepo;
    private MeterPersistRepo meterPersistRepo;
    private WarningProcessRepo warningProcessRepo;
    private ActionHandleRepo actionHandleRepo;

    @Override
    public void execution(MonitorMeta monitorMeta) {
        // 1. 获取通道并建立连接
        MonitorChannel monitorChannel = monitorChannelRepo
            .queryByChannelId(monitorMeta.getChannelMeta().getChannelId());
        monitorChannel.connect(monitorMeta.getChannelMeta());

        // 2. 执行命令
        String content = monitorChannel.execute(monitorMeta.getCommandMeta());

        // 3. 获取计算器并解析数据
        MeterCalculator meterCalculator = meterCalculatorRepo
            .queryById(monitorMeta.getMeterCalculatorMeta().getCalculatorId());
        Meter meter = meterCalculator.calculat(content);

        // 4. 告警处理与动作执行
        Action action = warningProcessRepo.handle(meter);
        ActionHandle actionHandle = actionHandleRepo.queryByAction(action.getId());
        actionHandle.execute();

        // 5. 数据持久化
        meterPersistRepo.insert(meter);
    }
}
```

#### Monitor 使用示例

```java
public class MonitorProcess {

    private MonitorRepo monitorRepo;

    public void handle(String monitorId) {
        // 1. 根据 ID 获取 Monitor 实现
        Monitor monitor = monitorRepo.queryById(monitorId);

        // 2. 构建元数据
        MonitorMeta meta = buildMonitorMeta();

        // 3. 执行监控
        monitor.execution(meta);
    }

    private MonitorMeta buildMonitorMeta() {
        MonitorMeta meta = new MonitorMeta();

        // 配置通道元数据
        ChannelMeta channelMeta = new ChannelMeta();
        channelMeta.setChannelId("esb_channel");
        channelMeta.setChannelVersion("1.0");
        meta.setChannelMeta(channelMeta);

        // 配置命令元数据
        CommandMeta commandMeta = new CommandMeta();
        commandMeta.setCommand("query_status");
        meta.setCommandMeta(commandMeta);

        // 配置计算器元数据
        MeterCalculatorMeta calculatorMeta = new MeterCalculatorMeta();
        calculatorMeta.setCalculatorId("default_calculator");
        meta.setMeterCalculatorMeta(calculatorMeta);

        return meta;
    }

    void test() {
        handle("user1");
    }
}
```

### MonitorChannel (监控通道)

定义监控数据的传输通道，支持 SSH 等多种实现。

```java
public interface MonitorChannel {
    void connect();
    String execute(String command);
    void disconnect();
}
```

### Meter (指标)

系统的指标定义，用于记录和追踪关键性能指标。

### MonitorCalculator (监控计算器)

执行指标计算和分析。

### Action & ActionHandle (动作与处理)

定义告警动作和处理机制，支持自定义处理逻辑。

## 快速开始

### 前置要求

- Java 17+
- Maven 3.6+

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd monitor

# 编译项目
mvn clean install

# 运行测试
mvn test

# 打包项目
mvn package
```

### 运行应用

```bash
# 方式1: 使用 Maven 运行
mvn spring-boot:run

# 方式2: 运行 JAR 包
java -jar target/monitor-0.0.1-SNAPSHOT.jar
```

## 使用示例

### 基本的监控流程

```java
// 获取监控器并执行
Monitor monitor = monitorRepo.queryById("user1");
monitor.execution();
```

### 扩展自定义监控器

1. 实现 `Monitor` 接口：

```java
public class CustomMonitor implements Monitor {
    @Override
    public void execution() {
        // 实现自定义监控逻辑
    }
}
```

2. 注册为 Spring Bean：

```java
@Configuration
public class MonitorConfig {
    @Bean
    public Monitor customMonitor() {
        return new CustomMonitor();
    }
}
```

### EsbMonitor 实现示例

`EsbMonitor` 是一个完整的监控器实现示例，展示了如何编排整个监控流程。以下是其工作流程：

```java
public class EsbMonitor implements Monitor {

    private MonitorChannelRepo monitorChannelRepo;
    private MonitorCalculatorRepo monitorCalculatorRepo;
    private MeterPersisRepo meterPersisRepo;
    private WarningProcessRepo warningProcessRepo;
    private ActionHandleRepo actionHandleRepo;

    @Override
    public void execution() {
        // 1. 获取并连接监控通道
        MonitorChannel monitorChannel = monitorChannelRepo.queryByChannelId("channelid");
        monitorChannel.connect();
        String content = monitorChannel.execute("cmd");

        // 2. 获取计算器并解析数据
        MonitorCalculator meterCalculator = monitorCalculatorRepo.queryById("calculator");
        Meter meter = meterCalculator.calculat(content);

        // 3. 处理告警并执行动作
        Action action = warningProcessRepo.handle(meter);
        ActionHandle actionHandle = actionHandleRepo.queryByAction(action.getId());
        actionHandle.execute();

        // 4. 持久化指标数据
        meterPersisRepo.insert(meter);
    }
}
```

**流程说明**:

1. **连接通道** (connect): 建立与远程系统（如 SSH）的连接
2. **执行命令** (execute): 在远程系统上执行监控命令
3. **数据计算** (calculat): 使用计算器解析命令结果并计算指标
4. **告警处理** (warningProcessRepo.handle): 根据指标判断是否触发告警
5. **动作执行** (actionHandle.execute): 执行相应的告警动作（邮件、短信等）
6. **数据持久化** (insert): 将指标数据保存到数据库

**依赖注入配置示例**:

```java
@Configuration
public class MonitorConfig {

    @Bean
    public Monitor esbMonitor(
        MonitorChannelRepo channelRepo,
        MonitorCalculatorRepo calculatorRepo,
        MeterPersisRepo meterRepo,
        WarningProcessRepo warningRepo,
        ActionHandleRepo actionRepo
    ) {
        EsbMonitor monitor = new EsbMonitor();
        monitor.setMonitorChannelRepo(channelRepo);
        monitor.setMonitorCalculatorRepo(calculatorRepo);
        monitor.setMeterPersisRepo(meterRepo);
        monitor.setWarningProcessRepo(warningRepo);
        monitor.setActionHandleRepo(actionRepo);
        return monitor;
    }
}
```

### 实现自定义通道

1. 实现 `MonitorChannel` 接口
2. 配置通道参数
3. 集成到监控流程

## 架构设计

### 分层架构概览

```
┌─────────────────────────────────────────────┐
│  Frameworks & Drivers Layer                 │
│  (Spring Boot, Database, Network)           │
├─────────────────────────────────────────────┤
│  Interface Adapters Layer                   │
│  ┌──────────┬──────────┬──────────────────┐ │
│  │ Monitor  │ Channel  │ MetersPersis     │ │
│  │ Adapter  │ Adapter  │ & Action Handler │ │
│  └──────────┴──────────┴──────────────────┘ │
├─────────────────────────────────────────────┤
│  Use Cases Layer                            │
│  (Monitor Execution Orchestration)          │
├─────────────────────────────────────────────┤
│  Entities Layer                             │
│  (Domain Models: Monitor, Channel, Meter)   │
└─────────────────────────────────────────────┘
```

### Clean Architecture 原则应用

#### 1. Entity Layer (核心领域层)
纯业务逻辑，零外部依赖：
- `Monitor`: 监控器接口定义
- `MonitorChannel`: 通道抽象接口
- `MonitorCalculator`: 计算器接口
- `Meter`: 指标值对象
- `Action`: 告警动作值对象
- `ActionHandle`: 动作处理器接口

**关键特性**:
- ✓ 完全独立的业务规则
- ✓ 无数据库/网络访问
- ✓ 支持单元测试无需外部系统
- ✓ 易于复用和组合

#### 2. Use Cases Layer (应用层)
应用级业务流程和编排：
- 监控执行流程协调
- 数据处理管道编排
- 告警决策和分发

#### 3. Interface Adapters Layer (适配层)
具体技术实现和数据格式转换：
- **monitor package**: 各种 Monitor 实现（EsbMonitor 等）
- **channel package**: 各种通道实现（SSH、本地 Shell 等）
- **meterpersisrepo package**: 数据库适配实现
- 数据格式转换和外部系统集成

#### 4. Frameworks & Drivers Layer (框架层)
具体框架和技术选择：
- Spring Boot 依赖注入容器
- 数据库驱动（MySQL、PostgreSQL 等）
- 网络库（SSH 客户端等）

### 低延迟性能优化

根据项目的性能要求（关键路径 < 1μs），遵循以下优化原则：

**资源管理**:
- 预分配关键资源，避免热路径内存分配
- 使用对象池模式复用 Meter 等高频对象
- 堆外内存存储大量指标数据

**并发优化**:
- 使用线程亲和性绑定监控线程到特定 CPU 核心
- 实现无锁数据结构用于指标缓存
- 减少上下文切换开销

**GC 优化**:
- 使用 G1GC 或 ZGC 低延迟垃圾回收器
- 配置最大 GC 暂停时间 ≤ 1ms
- 启用大页面支持提升 TLB 效率

## 配置

### 基础配置

应用配置文件位于 `src/main/resources/application.properties`：

```properties
# 应用配置
spring.application.name=monitor
server.port=8080

# 监控相关配置
monitor.enabled=true
monitor.execution-interval=5000  # 毫秒
```

### 数据库配置

```properties
# MySQL 数据源
spring.datasource.url=jdbc:mysql://localhost:3306/monitor_db
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### SSH 通道配置

```properties
# SSH 连接配置
ssh.host=192.168.1.100
ssh.port=22
ssh.username=admin
ssh.password=password
ssh.connection-timeout=10000  # 毫秒
```

### 低延迟 JVM 配置

```bash
# 推荐的生产环境参数
java -jar monitor.jar \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=1 \
  -XX:+AlwaysPreTouch \
  -XX:+DisableExplicitGC \
  -XX:+UseLargePages \
  -XX:LargePageSizeInBytes=2m \
  -Xms2G -Xmx2G \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:GuaranteedSafepointInterval=300000
```

## 扩展开发

### 添加新的监控器

在 `adapter/monitor` 目录下创建新的监控器实现，并通过依赖注入配置。

### 添加新的通道

在 `adapter/channel` 目录下实现 `MonitorChannel` 接口，支持新的传输方式。

### 集成新的数据源

在 `adapter/meterpersisrepo` 下实现 `MeterPersisRepo` 接口，支持新的数据库或存储系统。

## 测试

运行单元测试：

```bash
mvn test
```

查看测试覆盖率：

```bash
mvn test jacoco:report
```

## 依赖管理

### 主要依赖

- `spring-boot-starter`: Spring Boot 基础库
- `spring-boot-starter-test`: 测试支持

所有依赖版本由 Spring Boot 4.0.1 Parent POM 管理。

## 常见问题

### Q: 如何添加数据库支持？

A: 在 `pom.xml` 中添加数据库驱动依赖，然后在 `application.properties` 中配置数据源，最后实现相应的仓储接口。

### Q: 如何自定义告警动作？

A: 实现 `Action` 接口并通过 `ActionHandle` 进行处理，在配置中注册新的处理器。

### Q: 是否支持分布式监控？

A: 基础框架支持扩展，可以通过添加消息队列和分布式协调来实现。

## 性能考虑

根据项目的低延迟要求：

- 使用合适的 JVM 参数优化 GC
- 避免热路径中的内存分配
- 考虑使用堆外缓存减少 GC 压力

推荐 JVM 参数（参考 CLAUDE.md）：

```bash
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=1 \
-XX:+AlwaysPreTouch \
-XX:+DisableExplicitGC
```

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交修改 (`git commit -m 'Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 版本信息

- **当前版本**: 0.0.1-SNAPSHOT
- **Java 版本**: 17
- **Spring Boot 版本**: 4.0.1

## 许可证

待定（请在项目根目录的 LICENSE 文件中指定）

## 联系方式

- 项目维护者: tanggo.fund
- 反馈和问题: 请通过 Issue 追踪系统提交

---

**最后更新**: 2025-12-30
