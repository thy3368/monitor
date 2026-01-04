package com.tanggo.fund.monitor.core.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Metric {
    private String meterId;           // 指标ID
    private LocalDateTime timestamp;   // 采集时间
    private String metricName;        // 指标名称
    private double metricValue;       // 指标值
    private Map<String, String> tags; // 标签（用于分类和聚合）
//    private MeterStatus status;       // 指标状态（正常、告警等）
}
