package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.Collector;
import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.extension.MetricCalculator;
import com.tanggo.fund.monitor.core.extension.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;
import com.tanggo.fund.monitor.core.entity.meta.MetricCalculatorMeta;
import com.tanggo.fund.monitor.core.entity.meta.MetricRetrievalMeta;
import com.tanggo.fund.monitor.core.extension.collector.MetricCalculatorRepo;
import com.tanggo.fund.monitor.core.extension.collector.MetricPersistRepo;
import com.tanggo.fund.monitor.core.extension.collector.MetricRetrievalChannelRepo;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectorTemplate implements Collector {

    @Setter
    private MetricRetrievalChannelRepo monitorChannelRepo;

    @Setter
    private MetricCalculatorRepo metricCalculatorRepo;

    @Setter
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
    public void retrieval(MetricRetrievalMeta esbMetricRetrievalMeta) {

        log.info("开始指标检索流程");

        try {
            //获取通道
            log.debug("获取通道: {}", esbMetricRetrievalMeta.getChannelMeta().getChannelId());
            MetricRetrievalChannel metricRetrievalChannel = monitorChannelRepo.queryByChannelId(
                    esbMetricRetrievalMeta.getChannelMeta().getChannelId()
            );

            //建立连接
            log.debug("建立连接");
            metricRetrievalChannel.connect(esbMetricRetrievalMeta.getChannelMeta());

            //执行命令
            log.debug("执行命令: {}", esbMetricRetrievalMeta.getCommandMeta().getCommand());
            String content = metricRetrievalChannel.execute(esbMetricRetrievalMeta.getCommandMeta());
            log.debug("命令输出: {}", content);

            //获取数据处理
            log.debug("获取计算器: {}", esbMetricRetrievalMeta.getMetricCalculatorMeta().getCalculatorId());
            MetricCalculator metricCalculator = metricCalculatorRepo.queryById(
                    esbMetricRetrievalMeta.getMetricCalculatorMeta().getCalculatorId()
            );

            //数据解析并计算
            log.debug("开始数据解析和计算");
            Metric metric = metricCalculator.calculate(content);

            //入库
            log.debug("保存指标数据");
            metricPersistRepo.insert(metric);

            log.info("指标检索流程完成");
        } catch (Exception e) {
            log.error("指标检索流程异常", e);
            throw new RuntimeException("指标检索失败", e);
        }
    }


}
