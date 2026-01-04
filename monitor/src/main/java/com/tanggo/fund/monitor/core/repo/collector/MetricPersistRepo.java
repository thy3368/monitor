package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.Metric;

public interface MetricPersistRepo {
    void insert(Metric metric);
}
