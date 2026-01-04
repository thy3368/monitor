package com.tanggo.fund.monitor.plugin.repo;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;
import com.tanggo.fund.monitor.core.extension.collector.MetricRetrievalMetaRepo;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 内存实现的指标检索元数据仓储
 * 用于开发和测试环境
 */
@AllArgsConstructor
public class InMemoryMetricRetrievalMetaRepo implements MetricRetrievalMetaRepo {

    private final Map<String, MetricRetrievalMeta> metaData;

    @Override
    public MetricRetrievalMeta queryById(String monitorId) {
        return metaData.get(monitorId);
    }
}
