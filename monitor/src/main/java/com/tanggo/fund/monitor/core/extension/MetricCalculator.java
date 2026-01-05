package com.tanggo.fund.monitor.core.extension;

import com.tanggo.fund.monitor.core.entity.Metric;

public interface MetricCalculator {
    Metric calculate(String content);
}
