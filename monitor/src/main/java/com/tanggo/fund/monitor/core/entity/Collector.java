package com.tanggo.fund.monitor.core.entity;

import com.tanggo.fund.monitor.core.entity.meta.MeterRetrievalMeta;

public interface Collector {

    void meterRetrieval(MeterRetrievalMeta esbMeterRetrievalMeta);
}
