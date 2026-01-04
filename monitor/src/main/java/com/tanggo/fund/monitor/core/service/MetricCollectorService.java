package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;
import com.tanggo.fund.monitor.core.repo.collector.CollectorRepo;
import com.tanggo.fund.monitor.core.repo.collector.MetricRetrievalMetaRepo;

//执行选中的monitor
public class MetricCollectorService {


    private CollectorRepo collectorRepo;

    private MetricRetrievalMetaRepo metricRetrievalMetaRepo;

    public void handle(String monitorId) {

        MetricRetrievalMeta meta = metricRetrievalMetaRepo.queryById("esb_moni");

        CollectorTemplate collectorTemplate = new CollectorTemplate();

        collectorTemplate.meterRetrieval(meta);


    }


}
