package com.tanggo.fund.monitor.core.entity;

import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;

public interface MetricRetrievalChannel {
    void connect(ChannelMeta channelMeta);

    String execute(CommandMeta cmd);
}
