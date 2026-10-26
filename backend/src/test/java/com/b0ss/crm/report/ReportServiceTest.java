package com.b0ss.crm.report;

import com.b0ss.crm.customer.CustomerRepository;
import com.b0ss.crm.followup.FollowUpRepository;
import com.b0ss.crm.report.dto.ReportSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FollowUpRepository followUpRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void summaryCombinesAllThreeCounters() {
        when(customerRepository.count()).thenReturn(20L);
        when(customerRepository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(6L);
        when(followUpRepository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(9L);

        ReportSummary summary = reportService.summary();

        assertThat(summary.totalCustomers()).isEqualTo(20L);
        assertThat(summary.newCustomersThisMonth()).isEqualTo(6L);
        assertThat(summary.followUpsThisMonth()).isEqualTo(9L);
    }

    @Test
    void summaryUsesFirstDayOfCurrentMonthAsCutoff() {
        when(customerRepository.count()).thenReturn(0L);
        when(customerRepository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(0L);
        when(followUpRepository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(0L);

        reportService.summary();

        Instant expected = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(customerRepository).countByCreatedAtGreaterThanEqual(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expected);
    }
}
