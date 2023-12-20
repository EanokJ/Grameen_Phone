package com.grameenphone.wipro.task_executor.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grameenphone.wipro.task_executor.model.api.DueBillRequest;

import java.util.HashMap;
import java.util.Map;

public class DueBillRequestWrapper extends DueBillRequest {
    @JsonIgnore
    public int companyId;
    @JsonIgnore
    public int clientDivisionId;
    @JsonIgnore
    public int billDataId;
    @JsonIgnore
    public boolean hasBill;
    @JsonIgnore
    public String custMsisdn;
    @JsonIgnore
    public String billRevertibles;
}
