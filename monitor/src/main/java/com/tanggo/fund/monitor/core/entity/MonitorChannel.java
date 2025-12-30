package com.tanggo.fund.monitor.core.entity;

import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;

public interface MonitorChannel {
    void connect(ChannelMeta channelMeta);

    String execute(CommandMeta cmd);
}
