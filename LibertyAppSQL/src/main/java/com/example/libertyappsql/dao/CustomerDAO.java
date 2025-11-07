package com.example.libertyappsql.dao;

import com.example.libertyappsql.db.HibernateUtil;
import com.example.libertyappsql.model.Customer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class CustomerDAO {

    public List<Customer> getAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Customer> list = session.createQuery("FROM Customer", Customer.class).list();
        session.close();
        return list;
    }

    public void save(Customer customer) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.persist(customer);
        tx.commit();
        session.close();
    }
}
