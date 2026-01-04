package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.MetricRetrievalChannel;

public interface MetricRetrievalChannelRepo {
    MetricRetrievalChannel queryByChannelId(String channelId);
}
