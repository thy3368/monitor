package com.tanggo.fund.monitor.adapter.monitor;

import com.tanggo.fund.monitor.core.entity.*;
import com.tanggo.fund.monitor.core.repo.MeterPersisRepo;
import com.tanggo.fund.monitor.core.repo.MonitorCalculatorRepo;
import com.tanggo.fund.monitor.core.repo.MonitorChannelRepo;
import com.tanggo.fund.monitor.core.repo.WarningProcessRepo;

public class EsbMonitor implements Monitor {

    private MonitorChannelRepo monitorChannelRepo;

    private MonitorCalculatorRepo monitorCalculatorRepo;

    private MeterPersisRepo meterPersisRepo;

    private WarningProcessRepo warningProcessRepo;

    private ActionHandleRepo actionHandleRepo;


    @Override
    public void execution() {


        //获取通道
        MonitorChannel monitorChannel = monitorChannelRepo.queryByChannelId("channelid");
        //建立连接
        monitorChannel.connect();
        //执行命令
        String content = monitorChannel.execute("cmd");

        //获取数据处理
        MonitorCalculator monitorCalculator = monitorCalculatorRepo.queryById("calculator");
        //数据解析并计算
        Meter meter = monitorCalculator.calculat(content);

        //报警等行为处理
        Action action = warningProcessRepo.handle(meter);
        ActionHandle actionHandle = actionHandleRepo.queryByAction(action.getId());
        ActionHandle.execute();

        //入库
        meterPersisRepo.insert(meter);

    }
}
