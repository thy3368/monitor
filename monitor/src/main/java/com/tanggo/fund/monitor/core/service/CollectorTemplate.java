package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.Collector;
import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.entity.MetricCalculator;
import com.tanggo.fund.monitor.core.entity.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;
import com.tanggo.fund.monitor.core.entity.meta.MetricCalculatorMeta;
import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;
import com.tanggo.fund.monitor.core.repo.collector.MetricCalculatorRepo;
import com.tanggo.fund.monitor.core.repo.collector.MetricPersistRepo;
import com.tanggo.fund.monitor.core.repo.collector.MetricRetrievalChannelRepo;

public class CollectorTemplate implements Collector {

    private MetricRetrievalChannelRepo monitorChannelRepo;

    private MetricCalculatorRepo metricCalculatorRepo;

    private MetricPersistRepo metricPersistRepo;


    private MetricRetrievalMeta esbMonitorMeta() {

        MetricRetrievalMeta esbMetricRetrievalMeta = new MetricRetrievalMeta();

        // 初始化通道元数据
        ChannelMeta channelMeta = new ChannelMeta();
        channelMeta.setChannelId("esb_channel");
        channelMeta.setChannelVersion("1.0");
        esbMetricRetrievalMeta.setChannelMeta(channelMeta);

        // 初始化命令元数据
        CommandMeta commandMeta = new CommandMeta();
        commandMeta.setCommand("query_status");
        esbMetricRetrievalMeta.setCommandMeta(commandMeta);

        // 初始化计算器元数据
        MetricCalculatorMeta metricCalculatorMeta = new MetricCalculatorMeta();
        metricCalculatorMeta.setCalculatorId("default_calculator");
        esbMetricRetrievalMeta.setMetricCalculatorMeta(metricCalculatorMeta);

        return esbMetricRetrievalMeta;

    }

    @Override
    public void meterRetrieval(MetricRetrievalMeta esbMetricRetrievalMeta) {


        //获取通道
        MetricRetrievalChannel metricRetrievalChannel = monitorChannelRepo.queryByChannelId(esbMetricRetrievalMeta.getChannelMeta().getChannelId());
        //建立连接
        metricRetrievalChannel.connect(esbMetricRetrievalMeta.getChannelMeta());
        //执行命令
        String content = metricRetrievalChannel.execute(esbMetricRetrievalMeta.getCommandMeta());

        //获取数据处理
        MetricCalculator metricCalculator = metricCalculatorRepo.queryById(esbMetricRetrievalMeta.getMetricCalculatorMeta().getCalculatorId());
        //数据解析并计算
        Metric metric = metricCalculator.calculat(content);


        //入库
        metricPersistRepo.insert(metric);


    }


}
