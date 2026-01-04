package com.tanggo.fund.monitor.plugin.repo;

import com.tanggo.fund.monitor.core.entity.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.extension.collector.MetricRetrievalChannelRepo;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 内存实现的指标检索通道仓储
 * 用于开发和测试环境
 */
@AllArgsConstructor
public class InMemoryMetricRetrievalChannelRepo implements MetricRetrievalChannelRepo {

    private final Map<String, MetricRetrievalChannel> channels;

    @Override
    public MetricRetrievalChannel queryByChannelId(String channelId) {
        return channels.get(channelId);
    }
}
