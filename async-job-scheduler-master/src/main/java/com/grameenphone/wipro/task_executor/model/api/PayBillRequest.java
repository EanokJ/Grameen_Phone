package com.grameenphone.wipro.task_executor.model.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;
import java.util.Map;

public class PayBillRequest extends DueBillRequest {
    public String customer;
    public String pin;
    public String category;
    public String amount;
    public String bill;
    public String initiator;
    public Map<String, Object> params;

    //Required to insert prepaid bill into bill table
    @JsonIgnore
    public boolean hasBill;
    public int clientDivisionId;
    public int companyId;
    @JsonIgnore
    public int billDataId;
    @JsonIgnore
    public Timestamp taskCreationTime;
    public double vat;
    public double surviceCharge;

    public String companyMsisdn;

    public String companyName;
}
