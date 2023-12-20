package com.grameenphone.wipro.task_executor.model.orm.cbp;

import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author wipro.tribhuwan
 */
@Entity
@TypeDef(name = "safe_type", typeClass = SafeEnumType.class)
public class BillRevertibleCache {
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

    @OneToOne
    private Bill bill;
    private String valuesAsJson;
}