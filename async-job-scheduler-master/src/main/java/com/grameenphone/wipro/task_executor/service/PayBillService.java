package com.grameenphone.wipro.task_executor.service;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.dao.PayBillDao;
import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.model.api.PayBillRequest;
import com.grameenphone.wipro.task_executor.model.api.PayBillResponse;
import com.grameenphone.wipro.task_executor.model.entity.PaymentRequest;
import com.grameenphone.wipro.task_executor.model.entity.PaymentTask;
import com.grameenphone.wipro.task_executor.model.orm.cbp.WorkflowHop;
import com.grameenphone.wipro.task_executor.repository.CrudDao;
import com.grameenphone.wipro.task_executor.repository.CrudDao.ClosableCrudDao;
import com.grameenphone.wipro.task_executor.util.HttpClient;
import com.grameenphone.wipro.task_executor.util.HttpClient.HttpRequestSnapshot;
import com.grameenphone.wipro.task_executor.util.JsonUtil;
import com.grameenphone.wipro.task_executor.util.KV;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Service
public class PayBillService {
    private static final Logger logger = LoggerFactory.getLogger(PayBillService.class);
    private final static Integer TIMEOUT = Integer.valueOf(PropertyUtil.getProperty("api_timeout"));
    private final static String payBillUrl = PropertyUtil.getProperty("api_pay_bill_url");
    private final static String notificationUrl = PropertyUtil.getProperty("notification_pay_bill");
    private final static String instanceId = PropertyUtil.getProperty("task_executor_node_id");
    private static PayBillService service = null;
    private PayBillDao payBillDao = new PayBillDao();

    public PaymentTask getPaymentTask() {
        return payBillDao.getTopPendingPaymentTask();
    }

    public List<PayBillRequest> collectPendingBills(long reqHopId) {
        logger.debug("Collecting pending bill for Request Hop Id: " + reqHopId);
        return payBillDao.getPayableBills(reqHopId);
    }

    public PaymentRequest getPaymentRequest(long reqHopId) {
        logger.debug("Collecting Request Id for Request Hop Id: " + reqHopId);
        return payBillDao.getPaymentRequestId(reqHopId);
    }

    public boolean tryUpdateStatusToProcessing(PaymentTask paymentTask) {
        try (ClosableCrudDao<com.grameenphone.wipro.task_executor.model.orm.cbp.PaymentTask> paymentTaskDao = CrudDao.getNewSession(com.grameenphone.wipro.task_executor.model.orm.cbp.PaymentTask.class)) {
            if (paymentTaskDao.update("update PaymentTask set status = :tostatus, nodeId = :node, pin = null where id = :id and status = :fromstatus", new KV<>("tostatus", TaskStatus.Processing), new KV<>("node", instanceId), new KV<>("fromstatus", TaskStatus.Pending), new KV<>("id", paymentTask.id))) {
                paymentTask.status = TaskStatus.Processing;
                paymentTask.nodeId = instanceId;
                return true;
            }
        }
        return false;
    }

    public PayBillResponse payBill(PayBillRequest payBillRequest) throws IOException {
        logger.debug("Going to pay bill for Company: " + payBillRequest.company + " ConsumerId: " + payBillRequest.consumerId);
        HttpClient httpClient = new HttpClient(TIMEOUT);
        httpClient.setPayloadLoggerInterceptor((HttpRequestSnapshot x) -> x.body.replaceAll("(\"pin\")\\s*:\\s*\"[^\"]*\"|(\"password\")\\s*:\\s*\"[^\"]*\"", "$1$2: \"****\""));
        httpClient.setNoExceptionForError(true);
        return httpClient.postForEntity(payBillUrl, JsonUtil.toJson(payBillRequest), new HashMap<>() {{
            put("Content-Type", "application/json");
        }}, PayBillResponse.class);
    }

    public void updateBillStatus(String status, String accountNo, String billNo, int companyId, String mfsTxnId, Long timestamp, String errorMessage) {
        payBillDao.updateBillStatus(status, accountNo, billNo, companyId, mfsTxnId, timestamp, errorMessage);
    }

    public void updatePaymentTask(PaymentTask paymentTask) {
        payBillDao.updatePaymentTask(paymentTask);
    }

    public void sendWebNotification(long id) {
        Main.notificationSenderExecutors.submit(() -> {
            HttpClient httpClient = new HttpClient(1);
            try {
                httpClient.getForEntity(notificationUrl + "?rid=" + id, null);
            } catch (Exception e) {
            }
        });
    }

    public Integer insertAndUpdatePaymentRequestHop(long previousHopId, long wfHopId) {
        payBillDao.updateHopExecutionTime(previousHopId);
        return payBillDao.insertPaymentRequestHop(previousHopId, wfHopId);
    }

    public void updateHopIdInPaymentRequest(int hopId) {
        payBillDao.updateHopIdInPaymentRequest(hopId);
    }

    public long getWorkflowHopId(String code) {
        CrudDao<WorkflowHop> workflowHopCrudDao = CrudDao.get(WorkflowHop.class);
        return workflowHopCrudDao.findOneByAllMatches(new KV<>("code", code)).getId();
    }
}