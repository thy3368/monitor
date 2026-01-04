package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.Alert;
import com.tanggo.fund.monitor.core.entity.WarningRule;
import com.tanggo.fund.monitor.core.repo.warningrule.AlertRepo;
import com.tanggo.fund.monitor.core.repo.warningrule.WarningProcessRepo;
import com.tanggo.fund.monitor.core.repo.warningrule.WarningRuleRepo;

import java.util.List;

public class WarningRuleService {

    private AlertRepo alertRepo;

    private WarningRuleRepo warningRuleRepo;


    private WarningProcessRepo warningProcessRepo;


    public void processWarningRule() {
        List<WarningRule> warningRuleList = warningRuleRepo.loadAll();


        //报警等行为处理按规则
        Alert alert = warningProcessRepo.handle(warningRuleList);

        //下发警告
        alertRepo.sendAlert(alert);


    }

}
