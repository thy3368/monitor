package com.tanggo.fund.monitor.plugin.channel;

import com.tanggo.fund.monitor.core.extension.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;

public class FTPMetricRetrievalChannel implements MetricRetrievalChannel {


    @Override
    public void connect(ChannelMeta channelMeta) {

    }

    @Override
    public String execute(CommandMeta cmd) {
        return "";
    }
}
