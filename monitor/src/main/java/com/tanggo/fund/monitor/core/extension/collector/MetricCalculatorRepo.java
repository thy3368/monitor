package com.tanggo.fund.monitor.core.extension.collector;

import com.tanggo.fund.monitor.core.extension.MetricCalculator;

public interface MetricCalculatorRepo {
    MetricCalculator queryById(String calculator);
}
