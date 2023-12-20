package com.grameenphone.wipro.task_executor.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class Company {
	@Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    private int id;
    
    private String code;
	
    private String name;
    
    private Boolean hasBill;
    private String billRevertibles;

    @ManyToOne
    private Category category;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isHasBill() {
        return hasBill;
    }

    public void setHasBill(Boolean hasBill) {
        this.hasBill = hasBill;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getBillRevertibles() {
        return billRevertibles;
    }

    public void setBillRevertibles(String billRevertibles) {
        this.billRevertibles = billRevertibles;
    }
}