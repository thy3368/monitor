package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.Meter;

public interface MeterPersistRepo {
    void insert(Meter meter);
}
