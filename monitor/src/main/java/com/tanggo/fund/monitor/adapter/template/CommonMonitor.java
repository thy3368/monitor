package com.tanggo.fund.monitor.adapter.template;

import com.tanggo.fund.monitor.adapter.monitor.ActionHandleRepo;
import com.tanggo.fund.monitor.core.entity.*;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;
import com.tanggo.fund.monitor.core.entity.meta.MeterCalculatorMeta;
import com.tanggo.fund.monitor.core.entity.meta.MonitorMeta;
import com.tanggo.fund.monitor.core.repo.MeterCalculatorRepo;
import com.tanggo.fund.monitor.core.repo.MeterPersistRepo;
import com.tanggo.fund.monitor.core.repo.MonitorChannelRepo;
import com.tanggo.fund.monitor.core.repo.WarningProcessRepo;

public class CommonMonitor implements Monitor {

    private MonitorChannelRepo monitorChannelRepo;

    private MeterCalculatorRepo meterCalculatorRepo;

    private MeterPersistRepo meterPersistRepo;

    private WarningProcessRepo warningProcessRepo;

    private ActionHandleRepo actionHandleRepo;


    public void execution() {

        MonitorMeta esbMonitorMeta = esbMonitorMeta();
        execution(esbMonitorMeta);

    }

    private MonitorMeta esbMonitorMeta() {

        MonitorMeta esbMonitorMeta = new MonitorMeta();

        // 初始化通道元数据
        ChannelMeta channelMeta = new ChannelMeta();
        channelMeta.setChannelId("esb_channel");
        channelMeta.setChannelVersion("1.0");
        esbMonitorMeta.setChannelMeta(channelMeta);

        // 初始化命令元数据
        CommandMeta commandMeta = new CommandMeta();
        commandMeta.setCommand("query_status");
        esbMonitorMeta.setCommandMeta(commandMeta);

        // 初始化计算器元数据
        MeterCalculatorMeta meterCalculatorMeta = new MeterCalculatorMeta();
        meterCalculatorMeta.setCalculatorId("default_calculator");
        esbMonitorMeta.setMeterCalculatorMeta(meterCalculatorMeta);

        return esbMonitorMeta;

    }

    @Override
    public void execution(MonitorMeta esbMonitorMeta) {


        //获取通道
        MonitorChannel monitorChannel = monitorChannelRepo.queryByChannelId(esbMonitorMeta.getChannelMeta().getChannelId());
        //建立连接
        monitorChannel.connect(esbMonitorMeta.getChannelMeta());
        //执行命令
        String content = monitorChannel.execute(esbMonitorMeta.getCommandMeta());

        //获取数据处理
        MeterCalculator meterCalculator = meterCalculatorRepo.queryById(esbMonitorMeta.getMeterCalculatorMeta().getCalculatorId());
        //数据解析并计算
        Meter meter = meterCalculator.calculat(content);

        //报警等行为处理
        Action action = warningProcessRepo.handle(meter);
        ActionHandle actionHandle = actionHandleRepo.queryByAction(action.getId());
        actionHandle.execute();

        //入库
        meterPersistRepo.insert(meter);

    }
}
