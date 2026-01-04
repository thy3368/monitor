package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.MeterRetrievalChannel;

public interface MeterRetrievalChannelRepo {
    MeterRetrievalChannel queryByChannelId(String channelId);
}
