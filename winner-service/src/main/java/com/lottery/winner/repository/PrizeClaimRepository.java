package com.lottery.winner.repository;

import com.lottery.winner.entity.PrizeClaim;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrizeClaimRepository extends JpaRepository<PrizeClaim, Long> {

    boolean existsByWinnerId(Long winnerId);

    List<PrizeClaim> findByWinnerIdIn(Collection<Long> winnerIds);

    List<PrizeClaim> findByStatusOrderByIdAsc(String status);

    // Row lock: two admins can't decide the same claim at once
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrizeClaim> findForUpdateByClaimReference(String claimReference);
}