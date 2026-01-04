package com.tanggo.fund.monitor.core.entity;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;

public interface Collector {

    void meterRetrieval(MetricRetrievalMeta esbMetricRetrievalMeta);
}
