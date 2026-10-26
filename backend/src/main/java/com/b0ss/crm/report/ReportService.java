package com.b0ss.crm.report;

import com.b0ss.crm.customer.CustomerRepository;
import com.b0ss.crm.followup.FollowUpRepository;
import com.b0ss.crm.report.dto.ReportSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final FollowUpRepository followUpRepository;

    public ReportService(CustomerRepository customerRepository, FollowUpRepository followUpRepository) {
        this.customerRepository = customerRepository;
        this.followUpRepository = followUpRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummary summary() {
        Instant startOfMonth = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        return new ReportSummary(
                customerRepository.count(),
                customerRepository.countByCreatedAtGreaterThanEqual(startOfMonth),
                followUpRepository.countByCreatedAtGreaterThanEqual(startOfMonth));
    }
}
