package com.grameenphone.wipro.task_executor.dao;

import com.grameenphone.wipro.task_executor.config.CbpDbConnectionPool;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.model.api.PayBillRequest;
import com.grameenphone.wipro.task_executor.model.entity.PaymentRequest;
import com.grameenphone.wipro.task_executor.model.entity.PaymentTask;
import com.grameenphone.wipro.task_executor.util.CryptoUtil;
import com.grameenphone.wipro.task_executor.util.JsonUtil;
import com.grameenphone.wipro.task_executor.util.StringUtil;
import com.grameenphone.wipro.task_executor.util.jdbc.NamedParameterStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class PayBillDao {
    private static final Logger logger = LoggerFactory.getLogger(PayBillDao.class);

    public PaymentTask getTopPendingPaymentTask() {
        try(Connection con = CbpDbConnectionPool.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, request_hop_id, node_id, pin, update_time, status FROM payment_task WHERE status = '" + TaskStatus.Pending.name() + "' LIMIT 1")) {
            if (rs.next()) {
                PaymentTask paymentTask = new PaymentTask();
                paymentTask.id = rs.getLong(1);
                paymentTask.requestHopId = rs.getLong(2);
                Timestamp updateTime = rs.getTimestamp(5);
                String key = new SimpleDateFormat("YYYYMMdd::HHmmss").format(updateTime);
                try {
                    paymentTask.pin = CryptoUtil.decrypt("AES/CBC/PKCS5Padding", rs.getString(4), "AES", key, key);
                    //paymentTask.pin = "TestPin123";
                } catch(Throwable j) {
                    logger.error("Couldn't decrypt pin:: ", j);
                }
                return paymentTask;
            }
        } catch (Exception e) {
            logger.error("Error occurred to get payment task:: ", e);
        }
        return null;
    }

    public List<PayBillRequest> getPayableBills(long reqHopId) {
        List<PayBillRequest> payBillRequests = new ArrayList<>();
        try(Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, "SELECT a.msisdn, g.code, a.account_no, f.msisdn, h.name, a.bill_amount, a.bill_no, g.has_bill, e.id, g.id, i.create_time, f.id, r.values_as_json, a.service_charge, a.vat, g.msisdn, g.name \n" +
                "FROM bill a\n" +
                "JOIN payment_request_hop c ON c.request_id = a.request_id\n" +
                "JOIN payment_request i ON i.id = c.request_id\n" +
                "JOIN client_division e ON e.id = a.client_division_id\n" +
                "JOIN client f ON f.id = e.client_id\n" +
                "JOIN company g ON g.id = a.company_id\n" +
                "JOIN category h ON h.id = g.category_id\n" +
                "left JOIN bill_revertible_cache r ON a.id = r.bill_id\n" +
                "WHERE c.id = :hopId and a.status in (:unpaid, :fail)")) {
            pstmt.setLong("hopId", reqHopId);
            pstmt.setString("unpaid", BillStatus.Unpaid.name());
            pstmt.setString("fail", BillStatus.Fail.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	Double billAmount = rs.getDouble(6);
				if (billAmount == null || billAmount <= 0) {
					logger.info("Ignoring bill with bill amount 0 or null, bill number: " + rs.getString(7));
            		continue;
            	}
                PayBillRequest payBillRequest = new PayBillRequest();
                payBillRequest.customer = "0" + rs.getString(1);
                payBillRequest.channel = "CBP";
                payBillRequest.wallet_type = "RET";
                payBillRequest.company = rs.getString(2);
                payBillRequest.consumerId = rs.getString(3);
                payBillRequest.msisdn = "0" + rs.getString(4);
                payBillRequest.category = rs.getString(5);
                payBillRequest.amount = rs.getString(6);
                payBillRequest.bill = rs.getString(7);
                payBillRequest.hasBill = rs.getBoolean(8);
                payBillRequest.clientDivisionId = rs.getInt(9);
                payBillRequest.companyId = rs.getInt(10);
                payBillRequest.taskCreationTime = rs.getTimestamp(11);
                payBillRequest.initiator = rs.getString(12) + ":" + payBillRequest.clientDivisionId;
                String revertibles = rs.getString(13);
                payBillRequest.surviceCharge = rs.getDouble(14);
                payBillRequest.vat = rs.getDouble(15);
                payBillRequest.companyMsisdn = rs.getString(16);
                payBillRequest.companyName = rs.getString(17);
                if(revertibles != null) {
                    payBillRequest.params = JsonUtil.fromJson(revertibles, Map.class);
                }
                payBillRequests.add(payBillRequest);
            }
        } catch (Exception e) {
            logger.error("Error occurred to collecting pending bill:: ", e);
        }
        return payBillRequests;
    }

    public PaymentRequest getPaymentRequestId(long reqHopId) {
        try(Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, "SELECT i.create_time as time, c.request_id as id FROM payment_request_hop c JOIN payment_request i ON i.id = c.request_id WHERE c.id = :reqId")) {
            pstmt.setLong("reqId", reqHopId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.id = rs.getLong("id");
                paymentRequest.createTime = rs.getTimestamp("time");
                return paymentRequest;
            }
        } catch (Exception e) {
            logger.error("Error occurred to collecting pending bill:: ", e);
        }
        return null;
    }

    public void updatePaymentTask(PaymentTask paymentTask) {
        Connection con = null;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("UPDATE payment_task SET status=?, total_processed=?, success_count=?, dispute_count=?, failed_count=?, start_time=?, end_time=?, request_hop_id = ? WHERE id=?");
            pstmt.setString(1, paymentTask.status.name());
            pstmt.setInt(2, paymentTask.totalProcessed);
            pstmt.setInt(3, paymentTask.successCount);
            pstmt.setInt(4, paymentTask.disputeCount);
            pstmt.setInt(5, paymentTask.failedCount);
            pstmt.setTimestamp(6, paymentTask.startTime);
            pstmt.setTimestamp(7, paymentTask.endTime);
            pstmt.setLong(8, paymentTask.requestHopId);
            pstmt.setLong(9, paymentTask.id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error occurred to update payment task:: ", e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void updateBillStatus(String status, String accountNo, String billNo, int companyId, String mfsTxnId, Long timestamp, String errorMessage) {
        try(Connection con = CbpDbConnectionPool.getConnection()) {
            NamedParameterStatement pstmt = new NamedParameterStatement(con, "UPDATE bill SET status = :status, mfs_txnid = :txnid, payment_date = :date, error_message = :error WHERE bill_no = :bill AND company_id = :company and account_no = :account");
            pstmt.setString("status", status);
            pstmt.setString("txnid", mfsTxnId);
            pstmt.setString("bill", billNo);
            pstmt.setInt("company", companyId);
            pstmt.setString("account", accountNo);
            pstmt.setString("error", StringUtil.truncate(errorMessage, 250));
            pstmt.setTimestamp("date", timestamp == null ? null : new Timestamp(timestamp));
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error occurred while update bill status:: ", e);
        }
    }

    public int insertPaymentRequestHop(long previousHopId, long wfHopId) {
        Connection con = null;
        int id = 0;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO payment_request_hop (request_id, workflow_hop_id, previous_hop_id) VALUES((SELECT b.request_id FROM payment_request_hop b WHERE b.id = ?),?,?)", Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, previousHopId);
            pstmt.setLong(2, wfHopId);
            pstmt.setLong(3, previousHopId);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Error occurred while insert new hop:: ", e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
            return id;
        }
    }

    public void updateHopExecutionTime(long hopId) {
        Connection con = null;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("UPDATE payment_request_hop SET execution_time = ? WHERE id = ?");
            pstmt.setTimestamp(1, new Timestamp(Calendar.getInstance().getTimeInMillis()));
            pstmt.setLong(2, hopId);
            pstmt.executeUpdate();

        } catch (Exception e) {
            logger.error("Error occurred to update hop execution time:: ", e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void updateHopIdInPaymentRequest(int lastHopId) {
        Connection con = null;
        try {
            con = CbpDbConnectionPool.getConnection();
            PreparedStatement pstmt = con.prepareStatement("UPDATE payment_request a SET a.last_hop_id = ? WHERE a.id = (SELECT b.request_id FROM payment_request_hop b WHERE b.id = ?)");
            pstmt.setInt(1, lastHopId);
            pstmt.setInt(2, lastHopId);
            pstmt.executeUpdate();

        } catch (Exception e) {
            logger.error("Error occurred to update hop id in payment request table:: ", e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
}
