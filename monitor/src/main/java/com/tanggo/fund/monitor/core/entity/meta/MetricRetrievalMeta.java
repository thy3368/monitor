package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

@Data
public class MetricRetrievalMeta {

    private String monitorId;
    private ChannelMeta channelMeta;
    private CommandMeta commandMeta;
    private MetricCalculatorMeta metricCalculatorMeta;
}
