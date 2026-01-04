package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

import java.util.Map;

@Data
public class MetricCalculatorMeta {
    private String calculatorId;
    private Map<String, Object> extensions;
}
