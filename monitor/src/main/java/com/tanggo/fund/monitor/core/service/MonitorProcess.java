package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.meta.MeterRetrievalMeta;
import com.tanggo.fund.monitor.core.repo.collector.CollectorRepo;
import com.tanggo.fund.monitor.core.repo.collector.MeterRetrievalMetaRepo;

//执行选中的monitor
public class MonitorProcess {


    private CollectorRepo collectorRepo;

    private MeterRetrievalMetaRepo meterRetrievalMetaRepo;

    public void handle(String monitorId) {

        MeterRetrievalMeta meta = meterRetrievalMetaRepo.queryById("esb_moni");

        CommonCollector commonCollector = new CommonCollector();

        commonCollector.meterRetrieval(meta);


//        Collector collector = monitorRepo.queryById(meta.getMonitorId());
//
//        collector.meterRetrieval(meta);


    }


}
