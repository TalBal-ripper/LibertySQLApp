package com.example.libertyappsql.model;

import jakarta.persistence.*;

@Entity
@Table(name = "furniture")
public class Furniture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int furniture_id;

    private String title;
    private String type_name;
    private String material;
    private double price;
    private int quantity_in_stock;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    public Furniture() {}
}
