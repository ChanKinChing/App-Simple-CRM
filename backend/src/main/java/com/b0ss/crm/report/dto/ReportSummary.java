package com.b0ss.crm.report.dto;

public record ReportSummary(
        long totalCustomers,
        long newCustomersThisMonth,
        long followUpsThisMonth) {
}
