package com.tanggo.fund.monitor.core.repo;

import com.tanggo.fund.monitor.core.entity.Action;
import com.tanggo.fund.monitor.core.entity.Meter;

public interface WarningProcessRepo {
    Action handle(Meter meter);
}
