package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;

public interface CommissionService {
    CommissionAnalysisResponse runCommissionAnalysis();
}
