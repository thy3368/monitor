# Monitor - 插件化监控系统框架

一个基于 Spring Boot 和 Clean Architecture 的高性能、可扩展的监控系统框架，支持灵活的插件化架构。

## 项目概述

Monitor 是一个企业级监控解决方案框架，提供了以下核心功能：

- **模块化架构**: 遵循 Clean Architecture 设计原则，清晰的分层结构
- **插件化系统**: 支持自定义监控器、通道和持久化实现
- **多通道支持**: 内置 SSH 等监控通道，易于扩展
- **灵活的指标系统**: 支持多种指标（Meter）的定义和计算
- **告警处理**: 集成告警处理和动作执行机制

## 技术栈

- **Java**: 17
- **Spring Boot**: 4.0.1
- **Build Tool**: Maven
- **Architecture**: Clean Architecture

## 项目结构

```
src/
├── main/
│   └── java/com/tanggo/fund/monitor/
│       ├── core/                    # 核心领域层
│       │   ├── entity/              # 领域实体
│       │   │   ├── Monitor.java     # 监控器接口
│       │   │   ├── MonitorChannel.java  # 监控通道
│       │   │   ├── MonitorCalculator.java # 监控计算器
│       │   │   ├── Meter.java       # 指标
│       │   │   ├── Action.java      # 动作
│       │   │   └── ActionHandle.java # 动作处理器
│       │   └── repo/                # 仓储接口
│       │       ├── MonitorRepo.java
│       │       ├── MonitorChannelRepo.java
│       │       ├── MonitorCalculatorRepo.java
│       │       ├── MeterPersisRepo.java
│       │       └── WarningProcessRepo.java
│       ├── adapter/                 # 接口适配层
│       │   ├── monitor/             # 监控器实现
│       │   │   └── EsbMonitor.java
│       │   ├── channel/             # 通道实现
│       │   │   └── SSHMonitorChannel.java
│       │   └── meterpersisrepo/     # 持久化实现
│       │       └── MysqlMeterPersisRepo.java
│       ├── example/                 # 使用示例
│       │   └── MonitorProcess.java
│       └── MonitorApplication.java  # 应用入口
└── test/
    └── java/...                     # 测试代码
```

## 核心概念

### Monitor (监控器)
监控系统的基础单元，实现监控逻辑。

```java
public interface Monitor {
    void execution();
}
```

### MonitorChannel (监控通道)
定义监控数据的传输通道，支持 SSH 等多种实现。

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

## 配置

应用配置文件位于 `src/main/resources/application.properties`：

```properties
spring.application.name=monitor
```

根据具体需求添加数据库、SSH 连接等配置。

## 架构特点

### Clean Architecture 原则应用

- **Entity Layer**: 纯业务逻辑，无外部依赖
- **UseCase Layer**: 应用级业务规则和流程编排
- **Interface Adapters**: 监控器、通道等具体实现
- **Frameworks & Drivers**: Spring Boot 和外部库集成

### 低延迟性能优化

根据项目的性能要求，代码需要遵循以下原则：

- 预分配资源，避免运行时分配
- 使用线程亲和性优化并发处理
- 选择合适的 GC 策略

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
