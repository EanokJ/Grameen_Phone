package com.grameenphone.wipro.task_executor.model.api;

import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.Date;

@JsonRootName("response")
public class CheckBalanceResponse extends BaseResponse {
    public double balance;
    public String msisdn;
    public Date last_refill_on;
    public int last_refill_amount;
}
