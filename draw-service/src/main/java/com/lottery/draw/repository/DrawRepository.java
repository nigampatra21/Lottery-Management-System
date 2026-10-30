package com.lottery.draw.repository;

import com.lottery.draw.entity.Draw;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrawRepository extends JpaRepository<Draw, Long> {
    boolean existsByLotteryId(Long lotteryId);
    boolean existsByDrawId(String drawId);
    Optional<Draw> findByLotteryId(Long lotteryId);
    Optional<Draw> findByDrawId(String drawId);
}