package com.grameenphone.wipro.task_executor.model.api;

import java.util.Map;

public class DueBillRequest extends CPPBaseRequest {
    public String company;
    public String consumerId;
    public Map<String, String> params;
}