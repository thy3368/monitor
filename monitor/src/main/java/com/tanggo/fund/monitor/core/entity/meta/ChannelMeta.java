package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

import java.util.Map;

@Data
public class ChannelMeta {
    private String channelId;
    private String channelVersion;
    private Map<String, Object> extensions;
}
