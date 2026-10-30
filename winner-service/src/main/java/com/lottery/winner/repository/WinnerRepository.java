package com.lottery.winner.repository;

import com.lottery.winner.entity.Winner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WinnerRepository extends JpaRepository<Winner, Long> {
    boolean existsByDrawId(Long drawId);
    List<Winner> findByDrawIdOrderByPrizeAmountDescIdAsc(Long drawId);
    List<Winner> findByCustomerIdOrderByDeclaredAtDesc(Long customerId);
    Optional<Winner> findByWinnerId(String winnerId);
    List<Winner> findByIdIn(Collection<Long> ids);
    boolean existsByWinnerId(String id);
}