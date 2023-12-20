package com.grameenphone.wipro.task_executor.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.Collection;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Client {
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
    private String address1;
    private String address2;
    private int msisdn;
    private boolean active = true;
    private String description;

    @OneToMany()
    @JoinColumn(name = "client_id")
    private Collection<ClientDivision> clientDivisions;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int msisdn) {
        this.msisdn = msisdn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<ClientDivision> getClientDivisions() {
        return clientDivisions;
    }

    public void setClientDivisions(Collection<ClientDivision> clientDivision) {
        this.clientDivisions = clientDivision;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Client ? getId() != 0 ? getId() == ((Client) obj).getId() : super.equals(obj) : super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getId() != 0 ? (int)getId() : super.hashCode();
    }
}