package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.Alert;
import com.tanggo.fund.monitor.core.entity.AlertRule;
import com.tanggo.fund.monitor.core.extension.alert.AlertRepo;
import com.tanggo.fund.monitor.core.extension.alert.AlertProcessRepo;
import com.tanggo.fund.monitor.core.extension.alert.AlertRuleRepo;

import java.util.List;

public class AlertRuleService {

    private AlertRepo alertRepo;

    private AlertRuleRepo alertRuleRepo;


    private AlertProcessRepo alertProcessRepo;


    public void processWarningRule() {

        //获取所有规则
        List<AlertRule> alertRuleList = alertRuleRepo.loadAll();

        //处理规则 生成警告
        Alert alert = alertProcessRepo.handle(alertRuleList);

        //下发警告
        alertRepo.sendAlert(alert);


    }

}
