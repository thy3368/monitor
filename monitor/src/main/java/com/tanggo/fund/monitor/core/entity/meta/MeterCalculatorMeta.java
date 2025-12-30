package com.tanggo.fund.monitor.core.entity.meta;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class MeterCalculatorMeta {
    private String calculatorId;
    private Map<String, Objects> extensions;
}
