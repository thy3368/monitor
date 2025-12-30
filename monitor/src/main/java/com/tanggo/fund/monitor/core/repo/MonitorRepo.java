package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.Monitor;

public interface MonitorRepo {
    Monitor queryById(String monitorId);
}
