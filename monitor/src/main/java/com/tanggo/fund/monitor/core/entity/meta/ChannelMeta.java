package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class ChannelMeta {
    private String channelId;
    private String channelVersion;
    private Map<String, Objects> extensions;
}
