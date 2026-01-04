package com.tanggo.fund.monitor.core.repo.warningrule;

import com.tanggo.fund.monitor.core.entity.Alert;
import com.tanggo.fund.monitor.core.entity.WarningRule;

import java.util.List;

public interface WarningProcessRepo {
    Alert handle(List<WarningRule> warningRules);
}
