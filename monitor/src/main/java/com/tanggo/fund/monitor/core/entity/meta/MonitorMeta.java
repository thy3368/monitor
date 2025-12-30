package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

@Data
public class MonitorMeta {

    private String monitorId;
    private ChannelMeta channelMeta;
    private CommandMeta commandMeta;
    private MeterCalculatorMeta meterCalculatorMeta;
}
