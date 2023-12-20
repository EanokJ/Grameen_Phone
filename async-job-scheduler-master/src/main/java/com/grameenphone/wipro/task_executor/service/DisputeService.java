package com.grameenphone.wipro.task_executor.service;

import com.grameenphone.wipro.task_executor.dao.DisputeDao;
import com.grameenphone.wipro.task_executor.enums.BillPaymentStatus;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.model.orm.fmfs.BillPayServiceStatus;
import com.grameenphone.wipro.task_executor.processors.DisputeStatusChecker;
import com.grameenphone.wipro.task_executor.repository.CrudDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DisputeService {
    private static final Logger logger = LoggerFactory.getLogger(DisputeStatusChecker.class);
    private DisputeDao disputeDao = new DisputeDao();

    public void updateDisputeBillStatus(Bill disputedBill) {
        BillPayServiceStatus billPayServiceStatus = CrudDao.get(BillPayServiceStatus.class).query()
                .eqif(() -> disputedBill.getMfsTxnid() != null, "orderID", disputedBill.getMfsTxnid())
                .eqif(() -> disputedBill.getMfsTxnid() == null, "companyCode", disputedBill.getCompany().getCode())
                .eqif(() -> disputedBill.getMfsTxnid() == null, "billNo", disputedBill.getBillNo())
                .eqif(() -> disputedBill.getMfsTxnid() == null, "accountNo", disputedBill.getAccountNo())
                .first();
        if (billPayServiceStatus == null) {
            disputeDao.updateDisputeBillStatus(disputedBill, BillPaymentStatus.Fail, "Payment Not Found At Core");
		} else if (disputedBill.getBillDataId() == null && billPayServiceStatus.status.equals(BillPaymentStatus.Fail)) {
            disputeDao.updateDisputeBillStatus(disputedBill, BillStatus.Obsolete, "Bill with no billdata attachment and status fail.");
        } else if (billPayServiceStatus.status != BillPaymentStatus.Dispute) {
            disputeDao.updateDisputeBillStatus(disputedBill, billPayServiceStatus.status);
        }
    }

    public List<com.grameenphone.wipro.task_executor.model.orm.cbp.Bill> getAllDisputedBills() {
        CrudDao<com.grameenphone.wipro.task_executor.model.orm.cbp.Bill> billCrudDao = CrudDao.get(com.grameenphone.wipro.task_executor.model.orm.cbp.Bill.class);
        Calendar nowCalender = Calendar.getInstance();
        nowCalender.add(Calendar.DAY_OF_MONTH, -2);
        return billCrudDao.query()
                .eq("status", BillStatus.Dispute)
                .nn("paymentDate")
                .le("paymentDate", new Date())
                .ge("paymentDate", nowCalender.getTime())
                .list();
    }

    @Transactional(noRollbackFor = Throwable.class)
    public void updateDisputeBills() {
        logger.debug("Starting dispute thread.");
        try {
            List<Bill> disputedBills = getAllDisputedBills();
            for (Bill disputedBill : disputedBills) {
                updateDisputeBillStatus(disputedBill);
            }
        } catch (Throwable g) { //Exception is handled to prevent rollback
            logger.debug("Exception occurred in dispute updater process.", g);
        }
    }
}