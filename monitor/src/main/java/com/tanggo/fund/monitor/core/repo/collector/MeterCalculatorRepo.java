package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.MeterCalculator;

public interface MeterCalculatorRepo {
    MeterCalculator queryById(String calculator);
}
