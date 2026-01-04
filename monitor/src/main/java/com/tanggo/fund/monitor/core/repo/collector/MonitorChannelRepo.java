package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.MeterRetrievalChannel;

public interface MonitorChannelRepo {
    MeterRetrievalChannel queryByChannelId(String channelid);
}
