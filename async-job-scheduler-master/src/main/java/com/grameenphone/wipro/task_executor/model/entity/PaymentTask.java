package com.grameenphone.wipro.task_executor.model.entity;

import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.sql.Timestamp;

@Entity
@TypeDef(name = "safe_type", typeClass = SafeEnumType.class)
public class PaymentTask {
    public Long id;
    public Long requestHopId;
    public String nodeId;
    public String pin;
    @Enumerated(EnumType.STRING)
    @Type(type = "safe_type")
    public TaskStatus status = TaskStatus.Pending;
    public Integer totalProcessed;
    public Integer successCount;
    public Integer failedCount;
    public Integer disputeCount;
    public Timestamp startTime;
    public Timestamp endTime;

    @Transient
    public Timestamp creationTime;

    @Transient
    public Long requestId;
}