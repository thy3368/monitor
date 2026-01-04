package com.tanggo.fund.monitor.core.repo.warningrule;

import com.tanggo.fund.monitor.core.entity.Alert;
import com.tanggo.fund.monitor.core.entity.AlertRule;

import java.util.List;

public interface AlertProcessRepo {
    Alert handle(List<AlertRule> alertRules);
}
