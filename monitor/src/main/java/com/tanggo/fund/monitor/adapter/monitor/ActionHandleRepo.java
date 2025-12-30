package com.tanggo.fund.monitor.adapter.monitor;

import com.tanggo.fund.monitor.core.entity.ActionHandle;

public interface ActionHandleRepo {
    ActionHandle queryByAction(String id);
}
