package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.MonitorChannel;

public interface MonitorChannelRepo {
    MonitorChannel queryByChannelId(String channelid);
}
