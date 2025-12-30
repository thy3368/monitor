package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.MonitorCalculator;

public interface MonitorCalculatorRepo {
    MonitorCalculator queryById(String calculator);
}
