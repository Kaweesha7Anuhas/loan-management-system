package com.example.loanmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OfficerDashboardResponse {
    private long totalApplicationsCreated;
    private long submittedApplications; // Awaiting review
    private long underReviewApplications;
    private long recommendedApplications;
    private long approvedApplications;
    private long rejectedApplications;
}