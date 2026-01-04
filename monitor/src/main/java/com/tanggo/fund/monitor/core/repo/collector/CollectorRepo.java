package com.tanggo.fund.monitor.core.repo.collector;

import com.tanggo.fund.monitor.core.entity.Collector;

public interface CollectorRepo {
    Collector queryById(String monitorId);
}
