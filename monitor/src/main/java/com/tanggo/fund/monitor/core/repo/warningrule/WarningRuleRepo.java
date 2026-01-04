package com.tanggo.fund.monitor.core.repo.warningrule;

import com.tanggo.fund.monitor.core.entity.WarningRule;

import java.util.List;

public interface WarningRuleRepo {
    List<WarningRule> loadAll();
}
