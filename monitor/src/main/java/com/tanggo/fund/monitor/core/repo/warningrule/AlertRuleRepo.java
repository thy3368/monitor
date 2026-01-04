package com.tanggo.fund.monitor.core.repo.warningrule;

import com.tanggo.fund.monitor.core.entity.AlertRule;

import java.util.List;

public interface AlertRuleRepo {
    List<AlertRule> loadAll();
}
