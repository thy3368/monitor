package com.tanggo.fund.monitor.example;

import com.tanggo.fund.monitor.core.entity.Monitor;
import com.tanggo.fund.monitor.core.repo.MonitorRepo;

public class MonitorProcess {


    private MonitorRepo monitorRepo;


    public void handle(String monitorId) {

        Monitor monitor = monitorRepo.queryById(monitorId);

        monitor.execution();


    }


    void test1() {


        handle("user1");

    }

}
