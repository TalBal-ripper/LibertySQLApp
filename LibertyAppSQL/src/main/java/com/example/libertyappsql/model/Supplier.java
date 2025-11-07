package com.example.libertyappsql.model;

import jakarta.persistence.*;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int supplier_id;

    private String company_name;
    private String phone;
    private String city;

    public Supplier() {}
}

