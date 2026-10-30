package com.lottery.winner.controller;

import com.lottery.winner.dto.ClaimRequest;
import com.lottery.winner.dto.ClaimResponse;
import com.lottery.winner.security.AuthenticatedUser;
import com.lottery.winner.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Prize claims", description = "Customers claim, admins verify")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "Claim one of my prizes", description = "Role: CUSTOMER")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse submit(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ClaimRequest req) {
        return claimService.submit(user.userId(), req);
    }

    @Operation(summary = "My claims", description = "Role: CUSTOMER")
    @GetMapping("/my")
    public List<ClaimResponse> mine(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return claimService.mine(user.userId());
    }

    @Operation(summary = "List claims by status", description = "Role: ADMIN")
    @GetMapping
    public List<ClaimResponse> list(
            @Parameter(description = "PENDING, APPROVED or REJECTED")
            @RequestParam(defaultValue = "PENDING") String status) {
        return claimService.byStatus(status);
    }

    @Operation(summary = "Approve a claim",
            description = "Role: ADMIN. Decided once. You cannot verify your own claim")
    @PostMapping("/{claimReference}/approve")
    public ClaimResponse approve(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String claimReference) {
        return claimService.decide(user.userId(), claimReference, true);
    }

    @Operation(summary = "Reject a claim", description = "Role: ADMIN. Final")
    @PostMapping("/{claimReference}/reject")
    public ClaimResponse reject(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String claimReference) {
        return claimService.decide(user.userId(), claimReference, false);
    }
}