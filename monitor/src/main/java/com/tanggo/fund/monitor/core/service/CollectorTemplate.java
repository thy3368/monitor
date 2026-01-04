package com.tanggo.fund.monitor.core.service;

import com.tanggo.fund.monitor.core.entity.Collector;
import com.tanggo.fund.monitor.core.entity.Meter;
import com.tanggo.fund.monitor.core.entity.MeterCalculator;
import com.tanggo.fund.monitor.core.entity.MeterRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;
import com.tanggo.fund.monitor.core.entity.meta.MeterCalculatorMeta;
import com.tanggo.fund.monitor.core.entity.meta.MeterRetrievalMeta;
import com.tanggo.fund.monitor.core.repo.collector.MeterCalculatorRepo;
import com.tanggo.fund.monitor.core.repo.collector.MeterPersistRepo;
import com.tanggo.fund.monitor.core.repo.collector.MeterRetrievalChannelRepo;

public class CollectorTemplate implements Collector {

    private MeterRetrievalChannelRepo monitorChannelRepo;

    private MeterCalculatorRepo meterCalculatorRepo;

    private MeterPersistRepo meterPersistRepo;


    private MeterRetrievalMeta esbMonitorMeta() {

        MeterRetrievalMeta esbMeterRetrievalMeta = new MeterRetrievalMeta();

        // 初始化通道元数据
        ChannelMeta channelMeta = new ChannelMeta();
        channelMeta.setChannelId("esb_channel");
        channelMeta.setChannelVersion("1.0");
        esbMeterRetrievalMeta.setChannelMeta(channelMeta);

        // 初始化命令元数据
        CommandMeta commandMeta = new CommandMeta();
        commandMeta.setCommand("query_status");
        esbMeterRetrievalMeta.setCommandMeta(commandMeta);

        // 初始化计算器元数据
        MeterCalculatorMeta meterCalculatorMeta = new MeterCalculatorMeta();
        meterCalculatorMeta.setCalculatorId("default_calculator");
        esbMeterRetrievalMeta.setMeterCalculatorMeta(meterCalculatorMeta);

        return esbMeterRetrievalMeta;

    }

    @Override
    public void meterRetrieval(MeterRetrievalMeta esbMeterRetrievalMeta) {


        //获取通道
        MeterRetrievalChannel meterRetrievalChannel = monitorChannelRepo.queryByChannelId(esbMeterRetrievalMeta.getChannelMeta().getChannelId());
        //建立连接
        meterRetrievalChannel.connect(esbMeterRetrievalMeta.getChannelMeta());
        //执行命令
        String content = meterRetrievalChannel.execute(esbMeterRetrievalMeta.getCommandMeta());

        //获取数据处理
        MeterCalculator meterCalculator = meterCalculatorRepo.queryById(esbMeterRetrievalMeta.getMeterCalculatorMeta().getCalculatorId());
        //数据解析并计算
        Meter meter = meterCalculator.calculat(content);


        //入库
        meterPersistRepo.insert(meter);


    }


}
