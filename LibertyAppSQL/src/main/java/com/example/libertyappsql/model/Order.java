package com.example.libertyappsql.model;

import jakarta.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int order_id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "furniture_id")
    private Furniture furniture;

    private int quantity;
    private Date order_date;

    public Order() {}
}
