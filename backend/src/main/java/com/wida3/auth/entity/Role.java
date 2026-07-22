package com.wida3.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    private String name;

    protected Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
