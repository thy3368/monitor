package com.tanggo.fund.monitor.plugin.repo;

import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.extension.collector.MetricPersistRepo;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志实现的指标持久化仓储
 * 用于开发和测试环境，将指标打印到日志而不是数据库
 */
@Slf4j
public class LogMetricPersistRepo implements MetricPersistRepo {

    @Override
    public void insert(Metric metric) {
        log.info("=== 指标数据 ===");
        log.info("指标名称: {}", metric.getMetricName());
        log.info("指标值: {}", metric.getMetricValue());
        log.info("采集时间: {}", metric.getTimestamp());
        log.info("标签数据: {}", metric.getTags());
        log.info("================");
    }
}
