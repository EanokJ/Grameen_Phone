package com.grameenphone.wipro.task_executor.dao;

import com.grameenphone.wipro.task_executor.config.CbpDbConnectionPool;
import com.grameenphone.wipro.task_executor.enums.BillPaymentStatus;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.util.jdbc.NamedParameterStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class DisputeDao {
    private static final Logger logger = LoggerFactory.getLogger(DisputeDao.class);

    public void updateDisputeBillStatus(Bill bill, BillPaymentStatus status) {
        updateDisputeBillStatus(bill, status, null);
    }

    public void updateDisputeBillStatus(Bill bill, BillPaymentStatus status, String errorMessage) {
        updateDisputeBillStatus(bill, Enum.valueOf(BillStatus.class, status.name()), errorMessage);
    }

    public void updateDisputeBillStatus(Bill bill, BillStatus status, String errorMessage) {
        try(Connection con = CbpDbConnectionPool.getConnection()) {
            NamedParameterStatement pstmt = new NamedParameterStatement(con,"UPDATE bill SET status = :status, error_message = :error WHERE id = :id");
            pstmt.setString("status", status.name());
            pstmt.setLong("id", bill.getId());
            pstmt.setString("error", errorMessage);
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to update dispute status:: ", e);
        }
    }
}