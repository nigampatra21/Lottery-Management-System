package com.lottery.draw.repository;

import com.lottery.draw.entity.WinningNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WinningNumberRepository extends JpaRepository<WinningNumber, Long> {
    List<WinningNumber> findByDrawIdOrderBySequenceNumberAsc(Long drawId);
}