package com.grameenphone.wipro.task_executor.model.post_paid_due_bills;

import lombok.Data;

@Data
public class DueBillCommunicatorResponse {
    private String message;
    private int code;
    private String utility;
    private String account_no;
    private long timestamp;
    private BillList[] bill_list;
}
