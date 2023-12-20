package com.grameenphone.wipro.task_executor.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grameenphone.wipro.task_executor.config.CbpDbConnectionPool;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.model.orm.cbp.BillData;
import com.grameenphone.wipro.task_executor.model.orm.cbp.BillDetailTask;
import com.grameenphone.wipro.task_executor.model.wrapper.DueBillRequestWrapper;
import com.grameenphone.wipro.task_executor.repository.CrudDao;
import com.grameenphone.wipro.task_executor.util.jdbc.NamedParameterStatement;

@Repository
public class GetBillDao {
    private static final Logger logger = LoggerFactory.getLogger(GetBillDao.class);

    public List<DueBillRequestWrapper> getDueBillFetchRequests(BillDetailTask billDetailTask) {
        List<DueBillRequestWrapper> dueBillDbResponseList = new ArrayList<>();
        DueBillRequestWrapper dueBillRequestWrapper;
        try {
            CrudDao billDataDao = CrudDao.get(BillData.class);
            Collection<String> statusCollection = Arrays.asList("Validated", "RemovalPending");
            List<BillData> billDataList = billDataDao.query().eqif(() -> billDetailTask.getCompany() != null, "company", billDetailTask.getCompany())
                .eqif(() -> billDetailTask.getCompany() == null && billDetailTask.getCategory() != null, "company.category", billDetailTask.getCategory())
                .eqif(() -> billDetailTask.getAccountNo() != null, "accountNo", billDetailTask.getAccountNo())
                .inif(() -> billDetailTask.getClientDivisions().size() > 0, "clientDivision", billDetailTask.getClientDivisions())
                .in("status", statusCollection)
                .eqif(() -> billDetailTask.getClientDivisions().size() == 0, "clientDivision.client", billDetailTask.getClient()).list();
            logger.debug(billDataList.size() + " bill data collected");
            for (BillData billData : billDataList) {
                dueBillRequestWrapper = new DueBillRequestWrapper();
                dueBillRequestWrapper.billDataId = (int)billData.getId();
                dueBillRequestWrapper.consumerId = billData.getAccountNo();
                dueBillRequestWrapper.clientDivisionId = (int)billData.getClientDivision().getId();
                dueBillRequestWrapper.custMsisdn = "" + billData.getMsisdn();
                dueBillRequestWrapper.companyId = billData.getCompany().getId();
                dueBillRequestWrapper.company = billData.getCompany().getCode();
                dueBillRequestWrapper.hasBill = billData.getCompany().isHasBill();
                dueBillRequestWrapper.billRevertibles = billData.getCompany().getBillRevertibles();
                dueBillRequestWrapper.msisdn = "" + billDetailTask.getClient().getMsisdn();
                dueBillRequestWrapper.channel = "CBP";
                dueBillRequestWrapper.wallet_type = "RET";
				dueBillRequestWrapper.params = getAditionalFields((int)billData.getId());
                dueBillDbResponseList.add(dueBillRequestWrapper);
            }
        } catch (Exception e) {
            logger.error("Failed to collect due bill from DB:: ", e);
        }
        return dueBillDbResponseList;
    }

	public boolean insertOrUpdateBill(int clientDivisionId, int companyId, String consumerId, Double amount, Double serviceCharge, Double vat, String billNo, int billDataId, String msisdn, Timestamp dueDate) {
		String selectQuery = "SELECT client_division_id, company_id, account_no, bill_amount, service_charge, vat, bill_no, sync_date, status, bill_data_id, request_id, msisdn, due_date FROM bill WHERE company_id=? AND bill_no=? AND account_no=?";
		try (Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, selectQuery)) {
			pstmt.setInt(1, companyId);
			pstmt.setString(2, billNo);
			pstmt.setString(3, consumerId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				if (rs.getString("request_id") != null) {
					String prquery = "select wfh.code from payment_request pr inner join payment_request_hop prh on pr.last_hop_id = prh.id inner join workflow_hop wfh on prh.workflow_hop_id = wfh.id where pr.id=" + rs.getString("request_id");
					try (Statement stmt = con.createStatement()) {
						ResultSet prRs = stmt.executeQuery(prquery);
						while (prRs.next()) {
							if (!prRs.getString(1).equals("CPL") && !prRs.getString(1).equals("REJ")) {
								// Prevent Bill Status/Amount updating against BCR if the bill is attached with a payment request which is active (In Approval state or Waiting for Payment state)
								logger.debug("Preventing the bill update with billno: " + billNo);
								return false;
							} else {
								return updateBill(clientDivisionId, companyId, consumerId, amount, serviceCharge, vat, billNo, billDataId, msisdn, dueDate);
							}
						}
					}
				} else {
					return updateBill(clientDivisionId, companyId, consumerId, amount, serviceCharge, vat, billNo, billDataId, msisdn, dueDate);
				}
			} else {
				return insertBill(clientDivisionId, companyId, consumerId, amount, serviceCharge, vat, billNo, billDataId, msisdn, dueDate);
			}
		} catch (Exception e) {
			logger.error("Failed to insert/update in Bill Table:: ", e);
		}
		return false;
	}

	public boolean markAllUnpaidAsObsolete(int billDataId) {
		String query = "UPDATE bill b left outer JOIN payment_request pr ON b.request_id = pr.id left outer JOIN payment_request_hop prh on pr.last_hop_id = prh.id left outer join workflow_hop wfh on prh.workflow_hop_id = wfh.id set b.status = ? WHERE b.bill_data_id = ? AND b.status in (?,?) and (b.request_id IS NULL or wfh.code in ('CPL', 'REJ'))";
		try (Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, query)) {
			pstmt.setString(1, BillStatus.Obsolete.name());
			pstmt.setInt(2, billDataId);
			pstmt.setString(3, BillStatus.Unpaid.name());
			pstmt.setString(4, BillStatus.Fail.name());
			pstmt.executeUpdate();
			return true;
		} catch (Exception e) {
			logger.error("Failed to Mark unpaid bills as obsolete for billdata id :: " + billDataId, e);
			return false;
		}
	}

	private boolean updateBill(int clientDivisionId, int companyId, String consumerId, Double amount, Double serviceCharge, Double vat, String billNo, int billDataId, String msisdn, Timestamp dueDate) {
		String updateQuery = "UPDATE `bill` SET client_division_id=?, bill_amount=?, service_charge=?, vat=?, sync_date=?, status=?, bill_data_id=?, msisdn=?, due_date=? WHERE company_id=? AND bill_no=? AND account_no=? and status != ?";
		try (Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, updateQuery)) {
			pstmt.setInt(1, clientDivisionId);
			pstmt.setDouble(2, amount);
			pstmt.setDouble(3, serviceCharge);
			pstmt.setDouble(4, vat);
			pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
			pstmt.setString(6, BillStatus.Unpaid.name());
			pstmt.setInt(7, billDataId);
			pstmt.setString(8, msisdn);
			pstmt.setTimestamp(9, dueDate);
			pstmt.setInt(10, companyId);
			pstmt.setString(11, billNo);
			pstmt.setString(12, consumerId);
			pstmt.setString(13, BillStatus.Success.name());
			pstmt.executeUpdate();
			return true;
		} catch (Exception e) {
			logger.error("Failed to update the bill in Bill Table:: ", e);
			return false;
		}
	}
    
	private boolean insertBill(int clientDivisionId, int companyId, String consumerId, Double amount, Double serviceCharge, Double vat, String billNo, int billDataId, String msisdn, Timestamp dueDate) {
		String insertQuery = "INSERT into bill (client_division_id, company_id, account_no, bill_amount, service_charge, vat, bill_no, sync_date, status, bill_data_id, msisdn, due_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
		try (Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement insertPstmt = new NamedParameterStatement(con, insertQuery)) {
			insertPstmt.setInt(1, clientDivisionId);
			insertPstmt.setInt(2, companyId);
			insertPstmt.setString(3, consumerId);
			insertPstmt.setDouble(4, amount);
			insertPstmt.setDouble(5, serviceCharge);
			insertPstmt.setDouble(6, vat);
			insertPstmt.setString(7, billNo);
			insertPstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
			insertPstmt.setString(9, BillStatus.Unpaid.name());
			insertPstmt.setInt(10, billDataId);
			insertPstmt.setString(11, msisdn);
			insertPstmt.setTimestamp(12, dueDate);
			insertPstmt.executeUpdate();
			return true;
		} catch (Exception e) {
			logger.error("Failed to insert in Bill Table:: ", e);
			return false;
		}
	}
    
    public BillDetailTask getTopPendingBillCollectionTask() {
        CrudDao<BillDetailTask> billDetailTaskDao = CrudDao.get(BillDetailTask.class);
        return billDetailTaskDao.query().eq("status", TaskStatus.Pending).first();
    }

    @Transactional
    public void updateBillDetailTask(BillDetailTask billDetailTask) {
        CrudDao<BillDetailTask> billDetailTaskDao = CrudDao.get(BillDetailTask.class);
        billDetailTaskDao.update(billDetailTask);
    }

	public Map getAditionalFields(int billDataId){
		Map<String, String> params = new HashMap();
		String query = "select field.param_code, value.value from company_additional_fields field " +
				"join billdata_additional_field value join bill_data bill " +
				"on bill.company_id = field.bill_company_detail_id and " +
				"value.billdata_id = bill.id where bill.id = " + billDataId;

		try (Connection con = CbpDbConnectionPool.getConnection(); NamedParameterStatement pstmt = new NamedParameterStatement(con, query)) {
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()){
				params.put(rs.getString("param_code"), rs.getString("value"));
			}
		}catch (Exception e){
			logger.error("Error while getting addition field and values:"+e);
		}
		return params.size() > 0 ? params : new HashMap();
	}
}