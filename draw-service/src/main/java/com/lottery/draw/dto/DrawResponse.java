package com.lottery.draw.dto;

import com.lottery.draw.entity.Draw;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record DrawResponse(
        Long id, String drawId, Long lotteryId, String status,
        Instant scheduledAt, Instant startedAt, Instant completedAt,
        List<Integer> winningNumbers) {

    public static DrawResponse from(Draw d, List<Integer> numbers) {
        return new DrawResponse(d.getId(), d.getDrawId(), d.getLotteryId(),
                d.getStatus().name(), utc(d.getScheduledAt()),
                utc(d.getStartedAt()), utc(d.getCompletedAt()), numbers);
    }

    private static Instant utc(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }
}