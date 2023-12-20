package com.grameenphone.wipro.task_executor.model.orm.fmfs;

import com.grameenphone.wipro.task_executor.enums.BillPaymentStatus;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@Entity
@TypeDef(name = "safe_type", typeClass = SafeEnumType.class)
public class BillPayServiceStatus {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    public long id;
    public String accountNo;
    public String billNo;
    public String mfsTxnid;
    @Enumerated(EnumType.STRING)
    @Type(type = "safe_type")
    public BillPaymentStatus status;
    public String companyCode;

    @Column(name = "order_id")
    public String orderID;
}