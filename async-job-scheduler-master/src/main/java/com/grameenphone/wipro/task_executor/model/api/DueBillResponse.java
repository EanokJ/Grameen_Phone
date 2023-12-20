package com.grameenphone.wipro.task_executor.model.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DueBillResponse extends BaseResponse {
    public Response response;

    public static class Response {
        public String company;
        public String consumerId;
        public List<Bill> bills = new ArrayList<>();
    }

    public static class Bill {
        public String billNo;
        public String billMonthYear;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Dhaka")
        public Date billIssueDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Dhaka")
        public Timestamp billDueDate;
        public double amount;
        public Double vat;
        public double serviceCharge;
        public Map detail;
    }
}