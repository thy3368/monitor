package com.tanggo.fund.monitor.core.repo.warningrule;

import com.tanggo.fund.monitor.core.entity.Alert;

public interface AlertRepo {
    void sendAlert(Alert alert);
}
