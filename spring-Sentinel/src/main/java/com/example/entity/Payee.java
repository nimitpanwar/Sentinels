package com.example.entity;

import jakarta.persistence.*;

/** JPA entity for the 'payees' table. No Lombok - see entity/Transaction.java note. */
@Entity
@Table(name = "payees")
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payee_id")
    private Integer payeeId;

    @Column(name = "payee_name", nullable = false, length = 150)
    private String payeeName;

    @Column(name = "payee_identifier", nullable = false, length = 150)
    private String payeeIdentifier;

    public Payee() {
    }

    public Integer getPayeeId() { return payeeId; }
    public void setPayeeId(Integer payeeId) { this.payeeId = payeeId; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public String getPayeeIdentifier() { return payeeIdentifier; }
    public void setPayeeIdentifier(String payeeIdentifier) { this.payeeIdentifier = payeeIdentifier; }
}
