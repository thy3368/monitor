package com.tanggo.fund.monitor.core.entity;

public interface MonitorChannel {
    void connect();

    String execute(String cmd);
}
