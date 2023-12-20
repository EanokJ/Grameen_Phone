package com.grameenphone.wipro.task_executor.model.orm.cbp;

import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
@TypeDef(name = "safe_type", typeClass = SafeEnumType.class)
public class BillDetailTask {
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

    @Enumerated(EnumType.STRING)
    @Type(type = "safe_type")
    private TaskStatus status = TaskStatus.Pending;
    private String accountNo;

    @OneToMany(fetch = FetchType.EAGER)
    private Collection<ClientDivision> clientDivisions = new ArrayList<>();
      
    private String nodeId;
    private int totalProcessed;
    private int successCount;
    private int failedCount;
    private Date startTime;
    private Date endTime;
    private int addedById;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    @ManyToOne
    private Client client;

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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Collection<ClientDivision> getClientDivisions() {
        return clientDivisions;
    }

    public void setClientDivisions(Collection<ClientDivision> clientDivisions) {
        this.clientDivisions = clientDivisions;
    }

    public void addClientDivisions(ClientDivision clientDivisions) {
        this.clientDivisions.add(clientDivisions);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int getAddedById() {
        return addedById;
    }

    public void setAddedById(int addedById) {
        this.addedById = addedById;
    }
}