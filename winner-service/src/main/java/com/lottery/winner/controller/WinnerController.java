package com.lottery.winner.controller;

import com.lottery.winner.dto.DeclareRequest;
import com.lottery.winner.dto.DeclareResult;
import com.lottery.winner.dto.WinnerResponse;
import com.lottery.winner.security.AuthenticatedUser;
import com.lottery.winner.service.WinnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/winners")
@Tag(name = "Winners", description = "Declare winners (ADMIN) and view winnings (CUSTOMER)")
public class WinnerController {

    private final WinnerService winnerService;

    public WinnerController(WinnerService winnerService) {
        this.winnerService = winnerService;
    }

    @Operation(summary = "Declare winners for a draw",
            description = "Once per draw. Compares issued tickets with the winning numbers "
                    + "and splits each tier's pool equally, rounded down")
    @PostMapping("/declare")
    @ResponseStatus(HttpStatus.CREATED)
    public DeclareResult declare(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true) @RequestHeader("Authorization") String auth,
            @Valid @RequestBody DeclareRequest req) {
        return winnerService.declare(user.userId(), auth, req);
    }

    @Operation(summary = "Winners of a draw",
            description = "Use the public draw code, for example DRW-XXXXXXXXXXXX")
    @GetMapping("/draw/{drawId}")
    public List<WinnerResponse> forDraw(
            @Parameter(hidden = true) @RequestHeader("Authorization") String auth,
            @PathVariable String drawId) {
        return winnerService.forDraw(drawId, auth);
    }

    @Operation(summary = "My winnings")
    @GetMapping("/my")
    public List<WinnerResponse> mine(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return winnerService.mine(user.userId());
    }
}