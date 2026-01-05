package com.tanggo.fund.monitor.plugin.repo;

import com.tanggo.fund.monitor.core.extension.MetricCalculator;
import com.tanggo.fund.monitor.core.extension.collector.MetricCalculatorRepo;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 内存实现的指标计算器仓储
 * 用于开发和测试环境
 */
@AllArgsConstructor
public class InMemoryMetricCalculatorRepo implements MetricCalculatorRepo {

    private final Map<String, MetricCalculator> calculators;

    @Override
    public MetricCalculator queryById(String calculatorId) {
        return calculators.get(calculatorId);
    }
}
