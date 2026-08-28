package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
@Tag(name = "Commissions", description = "Endpoints for commission analysis and calculation")
public class CommissionController {

    private final CommissionService commissionService;

    @PostMapping("/run-analysis")
    @Operation(
            summary = "Trigger commission analysis operation",
            description = "Analyzes successful transactions and marks them commission worthy with 20% of the transaction fee."
    )
    public ResponseEntity<ApiResponse<CommissionAnalysisResponse>> runCommissionAnalysis() {
        log.info("Manual commission analysis triggered via REST endpoint");
        CommissionAnalysisResponse response = commissionService.runCommissionAnalysis();
        return ResponseEntity.ok(ApiResponse.success(response, "Commission analysis executed successfully"));
    }
}
