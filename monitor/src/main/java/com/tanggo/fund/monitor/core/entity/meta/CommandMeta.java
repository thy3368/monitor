package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

import java.util.Map;

@Data
public class CommandMeta {

    private String command;
    private Map<String, Object> extensions;

}
