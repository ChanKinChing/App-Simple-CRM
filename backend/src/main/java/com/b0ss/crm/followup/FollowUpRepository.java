package com.b0ss.crm.followup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    Page<FollowUp> findByCustomerId(Long customerId, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant from);
}
