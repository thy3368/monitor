package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;
import com.tanggo.fund.monitor.core.extension.collector.MetricRetrievalMetaRepo;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 指标收集服务
 * 执行选定的监控任务，获取和处理指标数据
 */
@Slf4j
public class MetricCollectorService {

    @Setter
    private CollectorTemplate collectorTemplate;

    @Setter
    private MetricRetrievalMetaRepo metricRetrievalMetaRepo;

    /**
     * 处理监控任务
     * 通过monitorId查询对应的指标检索配置，并执行收集任务
     *
     * @param monitorId 监控配置ID
     */
    public void handle(String monitorId) {
        log.info("开始处理监控任务: {}", monitorId);

        try {
            // 查询监控配置
            MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById(monitorId);

            if (meta == null) {
                log.error("找不到监控配置: {}", monitorId);
                return;
            }

            log.debug("已加载监控配置: channelId={}, command={}, calculatorId={}", meta.getChannelMeta().getChannelId(), meta.getCommandMeta().getCommand(), meta.getMetricCalculatorMeta().getCalculatorId());

            // 使用Collector执行指标检索
            collectorTemplate.retrieval(meta);

            log.info("监控任务完成: {}", monitorId);
        } catch (Exception e) {
            log.error("监控任务执行失败: {}", monitorId, e);
        }
    }

    /**
     * 处理默认监控任务（SSH CPU监控）
     * 使用预配置的ssh_cpu_monitor配置
     */
    public void handleSshCpuMonitor() {
        handle("ssh_cpu_monitor");
    }

    /**
     * 处理内存监控任务（SSH内存监控）
     * 使用预配置的ssh_memory_monitor配置
     */
    public void handleSshMemoryMonitor() {
        handle("ssh_memory_monitor");
    }

    /**
     * 处理所有监控任务（CPU和内存）
     * 同时执行SSH CPU监控和SSH内存监控
     */
    public void handleAllMonitors() {
        log.info("开始执行所有监控任务");
        handleSshCpuMonitor();
        handleSshMemoryMonitor();
        log.info("所有监控任务执行完成");
    }
}
