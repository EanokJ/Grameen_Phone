package com.grameenphone.wipro.task_executor.processors;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.model.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.service.DisputeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DisputeStatusChecker implements Runnable {
    DisputeService disputeService = Main.cbpContext.getBean(DisputeService.class);

    @Override
    public void run() {
        disputeService.updateDisputeBills();
    }
}