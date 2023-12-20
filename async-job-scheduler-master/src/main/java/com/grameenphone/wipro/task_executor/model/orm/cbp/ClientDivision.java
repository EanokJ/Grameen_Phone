package com.grameenphone.wipro.task_executor.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class ClientDivision {
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
    
	private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ClientDivision && getId() != 0 && ((ClientDivision)obj).getId() == getId()) || super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getId() != 0 ? (int)getId() : super.hashCode();
    }
}