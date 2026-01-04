package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;

public interface MetricRetrievalMetaRepo {
    MetricRetrievalMeta queryById(String esbMoni);
}
