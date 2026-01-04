package com.tanggo.fund.monitor.core.extension.collector;

import com.tanggo.fund.monitor.core.entity.Collector;

public interface CollectorRepo {
    Collector queryById(String monitorId);
}
