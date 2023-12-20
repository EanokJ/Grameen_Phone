package com.grameenphone.wipro.task_executor.model.orm.cbp;

import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * @author wipro.tribhuwan
 */
@Entity
@TypeDef(name = "safe_type", typeClass = SafeEnumType.class)
public class Bill {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    private long id;

    private String accountNo;
    private Double billAmount;
    private Double serviceCharge;
    private Double vat;
    private Date syncDate;
    private Date dueDate;
    private Date paymentDate;
    private String billNo;
    private String requestId;
    private int msisdn;
    private String mfsTxnid;
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Type(type = "safe_type")
    private BillStatus status;

    private Long billDataId;

    @ManyToOne
    private ClientDivision clientDivision;

    @ManyToOne
    private Company company;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Double getBillAmount() {
        return billAmount;
    }

    public void setBillAmount(Double billAmount) {
        this.billAmount = billAmount;
    }

    public Double getServiceCharge() {
        return serviceCharge == null ? 0 : serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public Double getVat() {
        return vat == null ? 0 : vat;
    }

    public void setVat(Double vat) {
        this.vat = vat;
    }

    public Date getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(Date syncDate) {
        this.syncDate = syncDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date syncDate) {
        this.dueDate = syncDate;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Long getBillDataId() {
        return billDataId;
    }

    public void setBillDataId(Long billDataId) {
        this.billDataId = billDataId;
    }

    public ClientDivision getClientDivision() {
        return clientDivision;
    }

    public void setClientDivision(ClientDivision clientDivision) {
        this.clientDivision = clientDivision;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int msisdn) {
        this.msisdn = msisdn;
    }

    public String getMfsTxnid() {
        return mfsTxnid;
    }

    public void setMfsTxnid(String mfsTxnid) {
        this.mfsTxnid = mfsTxnid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
}