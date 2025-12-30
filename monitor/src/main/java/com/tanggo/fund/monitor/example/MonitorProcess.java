package com.tanggo.fund.monitor.example;

import com.tanggo.fund.monitor.core.entity.Monitor;
import com.tanggo.fund.monitor.core.entity.meta.MonitorMeta;
import com.tanggo.fund.monitor.core.repo.MonitorMetaRepo;
import com.tanggo.fund.monitor.core.repo.MonitorRepo;

//执行选中的monitor
public class MonitorProcess {


    private MonitorRepo monitorRepo;

    private MonitorMetaRepo monitorMetaRepo;

    public void handle(String monitorId) {

        MonitorMeta meta = monitorMetaRepo.queryById("esb_moni");
        Monitor monitor = monitorRepo.queryById(meta.getMonitorId());

        monitor.execution(meta);


    }


}
