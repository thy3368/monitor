package com.tanggo.fund.monitor.core.extension.alert;

import com.tanggo.fund.monitor.core.entity.AlertRule;

import java.util.List;

public interface AlertRuleRepo {
    List<AlertRule> loadAll();
}
