package com.tanggo.fund.monitor.core.extension.collector;

import com.tanggo.fund.monitor.core.entity.MetricCalculator;

public interface MetricCalculatorRepo {
    MetricCalculator queryById(String calculator);
}
