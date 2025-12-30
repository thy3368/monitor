package com.tanggo.fund.monitor.core.entity;

import com.tanggo.fund.monitor.core.entity.meta.MonitorMeta;

public interface Monitor {

    void execution(MonitorMeta esbMonitorMeta);
}
