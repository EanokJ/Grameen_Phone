package com.grameenphone.wipro.task_executor.model.api;

public class PayBillResponse extends BaseResponse {
    public Response response;

    public static class Response {
        public String txnId;
        public String vendorTxnId;
        public String status;
        public String message;
        public String failCode;
    }
}
