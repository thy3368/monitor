# MySQL数据库持久化配置指南

## 概述

本项目支持将监控指标数据持久化到MySQL数据库，而不是仅将数据打印到日志。通过配置MySQL持久化，可以长期存储监控数据用于分析和查询。

## 前置准备

### 1. MySQL数据库安装
确保已安装MySQL 5.7或更高版本：

```bash
# 检查MySQL版本
mysql --version

# 启动MySQL服务
sudo service mysql start  # Linux
# 或
brew services start mysql  # macOS
```

### 2. 创建监控数据库

```bash
# 连接MySQL
mysql -u root -p

# 在MySQL客户端中执行
CREATE DATABASE monitor_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE monitor_db;

# 导入初始化SQL脚本
SOURCE /path/to/db/init_metrics_tables.sql;

# 验证表是否创建成功
SHOW TABLES;
```

## 配置步骤

### 1. 修改MySQL连接信息

编辑 `src/main/resources/spring-mysql-config.xml`：

```xml
<bean id="hikariConfig" class="com.zaxxer.hikari.HikariConfig">
    <!-- 修改为你的数据库地址和凭证 -->
    <property name="jdbcUrl" value="jdbc:mysql://YOUR_HOST:3306/monitor_db?useSSL=false&amp;serverTimezone=UTC&amp;characterEncoding=utf8mb4"/>
    <property name="username" value="YOUR_USERNAME"/>
    <property name="password" value="YOUR_PASSWORD"/>
    ...
</bean>
```

**关键参数说明**:
- `jdbcUrl`: MySQL服务器地址和端口，默认localhost:3306
- `username`: MySQL用户名，默认root
- `password`: MySQL密码
- `maximumPoolSize`: 最大连接数，建议10-20
- `minimumIdle`: 最小空闲连接数，建议5
- `connectionTimeout`: 连接超时时间（毫秒），默认30000

### 2. 选择持久化方式

#### 方案A：使用MySQL持久化（推荐用于生产环境）

编辑 `MonitorApplication.java`，使用MySQL配置：

```java
// 加载MySQL配置
ApplicationContext context = new ClassPathXmlApplicationContext(
    "spring-ssh-cpu-monitor.xml",
    "spring-mysql-config.xml"  // 添加MySQL配置
);
```

然后在 `spring-ssh-cpu-monitor.xml` 中，将日志持久化改为MySQL持久化：

```xml
<!-- 不使用日志持久化 -->
<!-- <bean id="metricPersistRepo" class="com.tanggo.fund.monitor.plugin.repo.LogMetricPersistRepo"/> -->

<!-- 使用MySQL持久化 -->
<bean id="metricPersistRepo" class="com.zaxxer.hikari.HikariDataSource">
    <!-- 从MySQL配置中导入 -->
    <property name="ref" value="mysqlMetricPersistRepo"/>
</bean>
```

#### 方案B：使用日志持久化（用于开发测试）

继续使用默认的日志实现，无需修改。

### 3. 编译和运行

```bash
# 编译项目
mvn clean compile

# 打包项目
mvn clean package -DskipTests

# 运行应用（使用MySQL持久化）
java -jar target/collector-0.0.1-SNAPSHOT.jar
```

## 数据库表结构

### 1. metrics 通用指标表

```sql
CREATE TABLE metrics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  metric_name VARCHAR(255) NOT NULL,      -- 指标名称 (cpu_usage, memory_usage等)
  metric_value DOUBLE NOT NULL,           -- 指标值
  timestamp DATETIME NOT NULL,            -- 采集时间
  tags JSON,                              -- 标签信息 (JSON格式)
  created_at TIMESTAMP DEFAULT NOW(),     -- 创建时间

  INDEX idx_metric_name (metric_name),
  INDEX idx_timestamp (timestamp)
);
```

### 2. metric_cpu_usage CPU监控表

```sql
CREATE TABLE metric_cpu_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cpu_usage_percent DOUBLE,       -- 总CPU使用率
  cpu_user_percent DOUBLE,        -- 用户模式CPU使用率
  cpu_system_percent DOUBLE,      -- 系统模式CPU使用率
  cpu_idle_percent DOUBLE,        -- CPU空闲率
  timestamp DATETIME NOT NULL,
  tags JSON,
  created_at TIMESTAMP DEFAULT NOW(),

  INDEX idx_timestamp (timestamp)
);
```

### 3. metric_memory_usage 内存监控表

```sql
CREATE TABLE metric_memory_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  memory_usage_percent DOUBLE,    -- 内存使用率
  memory_total DOUBLE,            -- 总内存
  memory_used DOUBLE,             -- 已用内存
  memory_free DOUBLE,             -- 空闲内存
  timestamp DATETIME NOT NULL,
  tags JSON,
  created_at TIMESTAMP DEFAULT NOW(),

  INDEX idx_timestamp (timestamp)
);
```

## 查询示例

### 1. 查看最近的CPU指标

```sql
SELECT * FROM metrics
WHERE metric_name = 'cpu_usage'
ORDER BY timestamp DESC
LIMIT 10;
```

### 2. 查看内存使用趋势（按小时聚合）

```sql
SELECT
  DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') AS hour,
  AVG(metric_value) AS avg_memory_usage,
  MAX(metric_value) AS max_memory_usage,
  MIN(metric_value) AS min_memory_usage,
  COUNT(*) AS sample_count
FROM metrics
WHERE metric_name = 'memory_usage'
  AND timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00')
ORDER BY hour DESC;
```

### 3. 导出指标数据为CSV

```sql
SELECT * FROM metrics
WHERE timestamp BETWEEN '2025-01-01' AND '2025-01-31'
INTO OUTFILE '/tmp/metrics_export.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
```

## 监控查询

### 实时监控面板查询

```sql
-- 获取最近10分钟的指标统计
SELECT
  metric_name,
  COUNT(*) as sample_count,
  AVG(metric_value) as avg_value,
  MAX(metric_value) as max_value,
  MIN(metric_value) as min_value,
  MAX(timestamp) as latest_time
FROM metrics
WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
GROUP BY metric_name;
```

### 性能分析查询

```sql
-- 找出CPU使用率最高的时间段
SELECT
  timestamp,
  metric_value as cpu_usage
FROM metrics
WHERE metric_name = 'cpu_usage'
ORDER BY metric_value DESC
LIMIT 20;

-- 找出内存压力时段
SELECT
  timestamp,
  metric_value as memory_usage
FROM metrics
WHERE metric_name = 'memory_usage'
  AND metric_value > 80
ORDER BY timestamp DESC
LIMIT 50;
```

## 性能优化建议

### 1. 索引优化

```sql
-- 为常用查询列创建复合索引
CREATE INDEX idx_metric_timestamp
ON metrics(metric_name, timestamp);

-- 为JSON标签查询创建索引
CREATE INDEX idx_tags
ON metrics((JSON_EXTRACT(tags, '$.host')));
```

### 2. 数据分区

对于大数据量，建议按时间分区：

```sql
ALTER TABLE metrics
PARTITION BY RANGE (YEAR(timestamp) * 10000 + MONTH(timestamp) * 100 + DAY(timestamp)) (
  PARTITION p20250101 VALUES LESS THAN (20250102),
  PARTITION p20250102 VALUES LESS THAN (20250103),
  PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### 3. 定期清理过期数据

```sql
-- 删除30天前的数据
DELETE FROM metrics
WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 使用事件定时清理
CREATE EVENT cleanup_metrics
ON SCHEDULE EVERY 1 DAY
DO
  DELETE FROM metrics
  WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 故障排查

### 问题1: 数据库连接失败

**症状**:
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**解决方案**:
1. 检查MySQL服务是否运行: `mysql -u root -p`
2. 检查数据库连接信息（主机、端口、用户名、密码）
3. 检查防火墙是否阻止MySQL端口(3306)

### 问题2: 表不存在

**症状**:
```
Table 'monitor_db.metrics' doesn't exist
```

**解决方案**:
1. 确认数据库已创建: `SHOW DATABASES;`
2. 运行初始化脚本: `SOURCE init_metrics_tables.sql;`

### 问题3: 连接池耗尽

**症状**:
```
HikariPool - Connection is not available, request timed out after 30000ms
```

**解决方案**:
1. 检查数据库连接是否被正确关闭
2. 增加 `maximumPoolSize`: 从10改为20
3. 检查是否有SQL语句执行缓慢

## 配置对比

| 特性 | 日志持久化 | MySQL持久化 |
|------|---------|-----------|
| 实现 | LogMetricPersistRepo | MysqlMetricPersistRepo |
| 适用场景 | 开发测试 | 生产环境 |
| 数据持久化 | 否 | 是 |
| 查询分析 | 困难 | 简单 |
| 长期存储 | 否 | 是 |
| 配置复杂度 | 低 | 中 |
| 依赖 | 无 | MySQL + HikariCP |

## 完整示例

### 启用MySQL持久化的完整步骤

1. **创建数据库和表**:
```bash
mysql -u root -p < db/init_metrics_tables.sql
```

2. **修改配置文件**:
```bash
# 编辑spring-mysql-config.xml中的数据库连接信息
vim src/main/resources/spring-mysql-config.xml
```

3. **修改应用入口**:
```java
// MonitorApplication.java
ApplicationContext context = new ClassPathXmlApplicationContext(
    "spring-ssh-cpu-monitor.xml",
    "spring-mysql-config.xml"
);
```

4. **编译运行**:
```bash
mvn clean package -DskipTests
java -jar target/collector-0.0.1-SNAPSHOT.jar
```

5. **验证数据保存**:
```bash
# 查看是否有数据写入
mysql -u root -p monitor_db -e "SELECT COUNT(*) FROM metrics;"
```

## 相关文件

- `db/init_metrics_tables.sql` - 数据库初始化脚本
- `src/main/resources/spring-mysql-config.xml` - MySQL配置文件
- `src/main/java/.../MysqlMetricPersistRepo.java` - MySQL持久化实现
- `pom.xml` - 已添加MySQL驱动和HikariCP依赖

## 最佳实践

1. **使用连接池**: 始终使用HikariCP等连接池，避免频繁建立连接
2. **异步写入**: 对于高频采集，考虑使用队列异步写入数据库
3. **定期备份**: 为重要的监控数据进行定期备份
4. **监控表大小**: 定期检查表大小，及时清理过期数据
5. **性能监测**: 使用EXPLAIN分析慢查询，建立必要索引

## 总结

通过配置MySQL持久化，你可以：
- ✅ 长期存储监控数据
- ✅ 灵活查询分析历史数据
- ✅ 生成性能报告和趋势分析
- ✅ 支持生产环境的高可靠性要求

MySQL配置现已完全集成到项目中，开箱即用！