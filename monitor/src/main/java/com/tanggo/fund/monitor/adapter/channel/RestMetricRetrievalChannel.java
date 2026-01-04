package com.tanggo.fund.monitor.adapter.channel;

import com.tanggo.fund.monitor.core.entity.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;

public class RestMetricRetrievalChannel implements MetricRetrievalChannel {


    @Override
    public void connect(ChannelMeta channelMeta) {

    }

    @Override
    public String execute(CommandMeta cmd) {
        return "";
    }
}
