package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.meta.MonitorMeta;

public interface MonitorMetaRepo {
    MonitorMeta queryById(String esbMoni);
}
