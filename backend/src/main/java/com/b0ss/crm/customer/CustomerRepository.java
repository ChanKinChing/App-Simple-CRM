package com.b0ss.crm.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            select c from Customer c
            where lower(c.name) like :pattern
               or lower(coalesce(c.company, '')) like :pattern
               or lower(coalesce(c.email, '')) like :pattern
               or coalesce(c.phone, '') like :pattern
            """)
    Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant from);
}
