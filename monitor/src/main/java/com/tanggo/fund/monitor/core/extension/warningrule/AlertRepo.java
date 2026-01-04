package com.tanggo.fund.monitor.core.extension.warningrule;

import com.tanggo.fund.monitor.core.entity.Alert;

public interface AlertRepo {
    void sendAlert(Alert alert);
}
