package com.grameenphone.wipro.task_executor.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grameenphone.wipro.task_executor.model.post_paid_due_bills.BillList;
import com.grameenphone.wipro.task_executor.model.post_paid_due_bills.DueBillCommunicatorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.dao.GetBillDao;
import com.grameenphone.wipro.task_executor.dao.UserDao;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.exception.HttpErrorResponseException;
import com.grameenphone.wipro.task_executor.model.api.DueBillRequest;
import com.grameenphone.wipro.task_executor.model.api.DueBillResponse;
import com.grameenphone.wipro.task_executor.model.entity.User;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.model.orm.cbp.BillDetailTask;
import com.grameenphone.wipro.task_executor.model.wrapper.DueBillRequestWrapper;
import com.grameenphone.wipro.task_executor.model.wrapper.DueBillResponseWrapper;
import com.grameenphone.wipro.task_executor.processors.MailThread;
import com.grameenphone.wipro.task_executor.repository.CrudDao;
import com.grameenphone.wipro.task_executor.repository.CrudDao.ClosableCrudDao;
import com.grameenphone.wipro.task_executor.util.HttpClient;
import com.grameenphone.wipro.task_executor.util.JsonUtil;
import com.grameenphone.wipro.task_executor.util.KV;
import com.grameenphone.wipro.task_executor.util.MacroReplacer;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;

@Service
public class BillCollectionService {
    private static final Logger logger = LoggerFactory.getLogger(BillCollectionService.class);

    @Value("${api_timeout}")
    private Integer apiTimeout;

    @Value("${api_due_bill_url}")
    private String dueBillUrl;

    @Value("${notification_due_bill}")
    private String notificationUrl;

    @Value("${task_executor_node_id}")
    private String instanceId;

    @Autowired
    private GetBillDao getBillDao;

    private UserDao userDao = new UserDao();

    public List<DueBillRequestWrapper> fetchBillDataList(BillDetailTask billDetailTask) {
        return getBillDao.getDueBillFetchRequests(billDetailTask);
    }

    public DueBillCommunicatorResponse getBillDetails(DueBillRequestWrapper dueBillRequestWrapper) throws IOException {
        DueBillRequest dueBillRequest = dueBillRequestWrapper;
        HttpClient httpClient = new HttpClient(apiTimeout);
        return httpClient.postForEntity(dueBillUrl, JsonUtil.toJson(dueBillRequest), new HashMap<>() {{
            put("Content-Type", "application/json");
        }}, DueBillCommunicatorResponse.class);
    }

    public boolean insertOrUpdateBill(int clientDivisionId, int companyId, String consumerId, Double amount, Double serviceCharge, Double vat, String billNo, int billDataId, String msisdn, Timestamp dueDate) {
        return getBillDao.insertOrUpdateBill(clientDivisionId, companyId, consumerId, amount, serviceCharge, vat, billNo, billDataId, msisdn, dueDate);
    }

    public void markAllUnpaidAsObsolete(int billDataId) {
        getBillDao.markAllUnpaidAsObsolete(billDataId);
    }

    public BillDetailTask getTopPendingBillCollectionTask() {
        return getBillDao.getTopPendingBillCollectionTask();
    }

    public boolean tryUpdateStatusToProcessing(BillDetailTask billDetailTask) {
        try (ClosableCrudDao<BillDetailTask> billDetailTaskDao = CrudDao.getNewSession(BillDetailTask.class)) {
            if (billDetailTaskDao.update("update BillDetailTask set status = :tostatus, nodeId = :node where id = :id and status = :fromstatus", new KV<>("tostatus", TaskStatus.Processing), new KV<>("node", instanceId), new KV<>("fromstatus", TaskStatus.Pending), new KV<>("id", billDetailTask.getId()))) {
                billDetailTask.setStatus(TaskStatus.Processing);
                billDetailTask.setNodeId(instanceId);
                return true;
            }
        }
        return false;
    }

    public void updateBillDetailTask(BillDetailTask billDetailTask) {
        getBillDao.updateBillDetailTask(billDetailTask);
    }

    public void sendWebNotification(long id) {
        Main.notificationSenderExecutors.submit(() -> {
            HttpClient httpClient = new HttpClient(10);
            try {
                httpClient.getForEntity(notificationUrl + "?rid=" + id, null);
            } catch (Exception e) {
            }
        });
    }

    public DueBillResponseWrapper collectDueBills(DueBillRequestWrapper dueBillRequestWrapper) {
        DueBillResponseWrapper dueBillResponseWrapper = null;
        try {
            dueBillResponseWrapper = new DueBillResponseWrapper(addToDueBillResponseObject(getBillDetails(dueBillRequestWrapper)));
            dueBillResponseWrapper.companyId = dueBillRequestWrapper.companyId;
            dueBillResponseWrapper.clientDivisionId = dueBillRequestWrapper.clientDivisionId;
            dueBillResponseWrapper.billDataId = dueBillRequestWrapper.billDataId;
            dueBillResponseWrapper.billRevertibles = dueBillRequestWrapper.billRevertibles;
        } catch (Exception e) {
            String message = "Failed to collect bill for company=" + dueBillRequestWrapper.company + ", account=" + dueBillRequestWrapper.consumerId + ", msisdn=" + dueBillRequestWrapper.msisdn;
            if (e instanceof HttpErrorResponseException) {
                logger.error(message + " With Status Code " + ((HttpErrorResponseException) e).getStatus());
            } else {
                logger.error(message + " Exception occurred:: ", e);
            }
        }
        return dueBillResponseWrapper;
    }

    public DueBillResponse addToDueBillResponseObject(DueBillCommunicatorResponse response) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        DueBillResponse dueBillResponse = new DueBillResponse();
        List<DueBillResponse.Bill> bills = new ArrayList<>();
        DueBillResponse.Response res = new DueBillResponse.Response();
        dueBillResponse.timestamp = response.getTimestamp();
        dueBillResponse.message = response.getMessage();
        dueBillResponse.status = response.getCode();
        res.company = response.getUtility();
        res.consumerId = response.getAccount_no();
        for (BillList list : response.getBill_list()) {
            DueBillResponse.Bill bill = new DueBillResponse.Bill();
            bill.billNo = list.getBill_number();
            bill.amount = list.getAmount();
            bill.serviceCharge = list.getService_charge();
            if(list.getDue_date() != null){
                Date date = formatter.parse(list.getDue_date());
                bill.billDueDate = new Timestamp(date.getTime());
            }
            bill.billMonthYear = list.getDetail() == null ? null : list.getDetail().getBillMonth() + "-" + list.getDetail().getBillYear();
            if (list.getDetail() != null && list.getDetail().getIssueDate() != null) {
                String dateArr[] = list.getDetail().getIssueDate().split("-");
                Date date1 = formatter.parse(dateArr[2] + "-" + dateArr[1] + "-" + dateArr[0]);
                bill.billIssueDate = date1;
            }
            bill.vat = list.getDetail() != null ? Double.parseDouble(list.getDetail().getVat()) : null;
            bill.detail = new ObjectMapper().convertValue(list.getDetail(), new TypeReference<Map<String, Object>>() {});
            bills.add(bill);
        }
        res.bills = bills;
        dueBillResponse.response = res;
        return dueBillResponse;
    }

    public boolean hasAnyUnpaidOrDisputeBill(long billDataId) {
        CrudDao<com.grameenphone.wipro.task_executor.model.orm.cbp.Bill> billDao = CrudDao.get(com.grameenphone.wipro.task_executor.model.orm.cbp.Bill.class);
        return billDao.query().eq("billDataId", billDataId).in("status", Arrays.asList(BillStatus.Unpaid, BillStatus.Fail, BillStatus.Dispute)).count() > 0;
    }

    public void collectDueBills() {
        logger.debug("Starting get bill thread.");
        BillDetailTask billDetailTask;
        try {
            while ((billDetailTask = getTopPendingBillCollectionTask()) != null) {
                //To process from only one node whoever first can change the task to processing
                if (!tryUpdateStatusToProcessing(billDetailTask)) {
                    continue;
                }
                int totalProcessed = 0;
                int success = 0;
                int fail = 0;
                int billFound = 0;
                billDetailTask.setStartTime(new Timestamp(System.currentTimeMillis()));
                try {
                    logger.debug("processing bill collection for " + billDetailTask.getId());
                    List<DueBillRequestWrapper> dueBillRequestWrappers = fetchBillDataList(billDetailTask);
                    logger.debug(dueBillRequestWrappers.size() + " bill data found to collect bills ");
                    for (DueBillRequestWrapper dueBillRequestWrapper : dueBillRequestWrappers) {
                        logger.debug("Collecting bills for bill data " + dueBillRequestWrapper.billDataId);
                        totalProcessed++;
                        if (dueBillRequestWrapper.hasBill) {
                            DueBillResponseWrapper dueBillResponseWrapper = collectDueBills(dueBillRequestWrapper);
                            if (dueBillResponseWrapper != null) { //No Error occurred in bill collection process
                                success++;
                                logger.debug(dueBillResponseWrapper.response.bills.size() + " bills collected for bill data " + dueBillRequestWrapper.billDataId);
                                markAllUnpaidAsObsolete(dueBillRequestWrapper.billDataId);
                                if (dueBillResponseWrapper.response.bills.size() > 0) {
                                    for (DueBillResponse.Bill bill : dueBillResponseWrapper.response.bills) {
                                        if (bill.amount != 0 && insertOrUpdateBill(dueBillResponseWrapper.clientDivisionId, dueBillResponseWrapper.companyId, dueBillResponseWrapper.response.consumerId, bill.amount, bill.serviceCharge, bill.vat, bill.billNo, dueBillResponseWrapper.billDataId, dueBillRequestWrapper.custMsisdn, bill.billDueDate)) {
                                            billFound++;
                                            updateRevertibles(dueBillResponseWrapper, dueBillResponseWrapper.companyId, bill);
                                        }
                                    }
                                }
                            } else {
                                fail++;
                            }
                        } else {
                            if (!hasAnyUnpaidOrDisputeBill(dueBillRequestWrapper.billDataId)) {
                                logger.debug("Inserting new bill for bill data " + dueBillRequestWrapper.billDataId);
                                insertOrUpdateBill(dueBillRequestWrapper.clientDivisionId, dueBillRequestWrapper.companyId, dueBillRequestWrapper.consumerId, null, null, null, instanceId + new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date()) + ((System.nanoTime() % 1000000) / 1000), dueBillRequestWrapper.billDataId, dueBillRequestWrapper.custMsisdn, null);
                            } else {
                                logger.debug("There is already an unpaid or dispute bill for bill data " + dueBillRequestWrapper.billDataId);
                            }
                            success++;
                            billFound++;
                        }
                    }
                    logger.debug("Bill Collection: Processed {}, Success {}, Fail {}, Bill Found {}", totalProcessed, success, fail, billFound);
                } catch (Exception e) {
                    logger.error("Exception occured while refreshing bill data:: ", e);
                } finally {
                    billDetailTask.setEndTime(new Timestamp(System.currentTimeMillis()));
                    billDetailTask.setSuccessCount(success);
                    billDetailTask.setFailedCount(fail);
                    billDetailTask.setTotalProcessed(totalProcessed);
                    billDetailTask.setStatus(TaskStatus.Completed);
                    updateBillDetailTask(billDetailTask);
                    sendWebNotification(billDetailTask.getId());
                    sendMail(billDetailTask, billFound);
                }
            }
        } catch (Throwable g) { //Exception is handled to prevent rollback
            logger.debug("Exception occurred in bill collection process.", g);
        }
    }

    private void sendMail(BillDetailTask billDetailTask, int billFound) {
        String tempMailBody = PropertyUtil.getProperty("email-template-due-bill");
        String emailSubject = PropertyUtil.getProperty("subject-template-due-bill");
        User user = userDao.findById(billDetailTask.getAddedById());
        String modifiedMailBody = null;
        try {
            modifiedMailBody = MacroReplacer.replaceMacros(tempMailBody, new KV<>("user", user), new KV<>("bill_found", billFound), new KV<>("task", billDetailTask), new KV<>("request_id", "BCR" + String.format("%08x", billDetailTask.getId()).toUpperCase()));
        } catch (Exception e) {
            logger.error("Macro replacing Error::" + e.getMessage());
        }
        Main.notificationSenderExecutors.submit(new MailThread(modifiedMailBody, user.emailAddress, "", emailSubject));
    }

    private void updateRevertibles(DueBillResponseWrapper dueBillResponseWrapper, int companyId, DueBillResponse.Bill bill) {
        if (dueBillResponseWrapper.billRevertibles != null) {
            Map savables = new HashMap();
            for (String key : dueBillResponseWrapper.billRevertibles.split(",")) {
                Object value = bill.detail.get(key);
                savables.put(key, value);
            }
            try {
                String savablesAsJson = JsonUtil.toJson(savables);
                CrudDao<Bill> billCrudDao = CrudDao.get(Bill.class);
                billCrudDao.updateSql("delete bc from bill_revertible_cache bc inner join bill b on bc.bill_id = b.id where b.bill_no = :no and b.company_id = :com", new KV<>("no", bill.billNo), new KV<>("com", companyId));
                billCrudDao.update("insert into BillRevertibleCache (bill, valuesAsJson) select b, :value from Bill b where b.billNo = :no and b.company.id = :com", new KV<>("no", bill.billNo), new KV<>("com", companyId), new KV<>("value", savablesAsJson));
            } catch (JsonProcessingException e) {
                logger.error("Couldn't save revertibles for the bills");
            }
        }
    }
}