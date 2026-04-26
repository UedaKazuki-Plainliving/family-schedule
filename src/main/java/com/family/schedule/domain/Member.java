package com.family.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "members_id_seq")
    @SequenceGenerator(name = "members_id_seq", sequenceName = "members_id_seq", allocationSize = 1)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected Member() {}

    public Member(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Integer getDisplayOrder() { return displayOrder; }

    public void setName(String name) { this.name = name; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
