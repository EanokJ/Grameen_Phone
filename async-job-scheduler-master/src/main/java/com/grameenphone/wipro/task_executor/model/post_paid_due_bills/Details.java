package com.grameenphone.wipro.task_executor.model.post_paid_due_bills;

import lombok.Data;

@Data
public class Details {
    private String accountNo;
    private String billNo;
    private String billMonth;
    private String billYear;
    private String totalKwh;
    private String amount;
    private String lpc;
    private String vat;
    private String issueDate;
    private String dueDate;
    private String paymentStatus;

}
