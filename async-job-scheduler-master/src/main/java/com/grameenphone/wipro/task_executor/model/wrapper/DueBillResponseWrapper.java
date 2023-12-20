package com.grameenphone.wipro.task_executor.model.wrapper;

import com.grameenphone.wipro.task_executor.model.api.DueBillResponse;

import java.util.ArrayList;
import java.util.List;

public class DueBillResponseWrapper extends DueBillResponse {
    public int companyId;
    public int clientDivisionId;
    public int billDataId;
    public String billRevertibles;

    public DueBillResponseWrapper() {
    }

    public DueBillResponseWrapper(DueBillResponse dueBillResponse) {
        this.timestamp = dueBillResponse.timestamp;
        this.status = dueBillResponse.status;
        this.message = dueBillResponse.message;
        this.response = dueBillResponse.response;
    }
}
