package com.tanggo.fund.monitor.core.extension.collector;

import com.tanggo.fund.monitor.core.entity.Metric;

public interface MetricPersistRepo {
    void insert(Metric metric);
}
