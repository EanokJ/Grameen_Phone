package com.grameenphone.wipro.task_executor.processors;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.service.BillCollectionService;

public class BillCollector implements Runnable {
    private BillCollectionService billCollectionService = Main.cbpContext.getBean(BillCollectionService.class);

    @Override
    public void run() {
        billCollectionService.collectDueBills();
    }
}