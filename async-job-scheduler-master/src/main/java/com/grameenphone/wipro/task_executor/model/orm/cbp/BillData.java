package com.grameenphone.wipro.task_executor.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
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
public class BillData {
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

    private Date createTime;

    private Date updateTime;

    private String accountNo;

    private String status;

    private int msisdn;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClientDivision clientDivision;

    private int addedById;

    private int updatedById;

    @ManyToOne
    private Company company;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int msisdn) {
        this.msisdn = msisdn;
    }

    public ClientDivision getClientDivision() {
        return clientDivision;
    }

    public void setClientDivision(ClientDivision clientDivision) {
        this.clientDivision = clientDivision;
    }

    public int getAddedById() {
        return addedById;
    }

    public void setAddedById(int addedById) {
        this.addedById = addedById;
    }

    public int getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(int updatedById) {
        this.updatedById = updatedById;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}