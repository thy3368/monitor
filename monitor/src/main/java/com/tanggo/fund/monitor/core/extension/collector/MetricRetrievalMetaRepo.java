package com.tanggo.fund.monitor.core.extension.collector;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;

public interface MetricRetrievalMetaRepo {
    MetricRetrievalMeta queryById(String esbMoni);
}
