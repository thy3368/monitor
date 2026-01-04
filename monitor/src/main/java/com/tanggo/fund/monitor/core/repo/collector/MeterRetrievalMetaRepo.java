package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.meta.MeterRetrievalMeta;

public interface MeterRetrievalMetaRepo {
    MeterRetrievalMeta queryById(String esbMoni);
}
