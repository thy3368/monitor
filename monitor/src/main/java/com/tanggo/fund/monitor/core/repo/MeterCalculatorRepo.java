package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.MeterCalculator;

public interface MeterCalculatorRepo {
    MeterCalculator queryById(String calculator);
}
