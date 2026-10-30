package com.lottery.draw.service;

import com.lottery.draw.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class WinningNumberGenerator {

    private final SecureRandom random = new SecureRandom();

    /** Returns `count` distinct numbers in [min, max], in the order drawn. */
    public List<Integer> generate(int count, int min, int max) {
        long range = (long) max - min + 1;
        if (count <= 0 || range < count) {
            throw new BusinessRuleException("The lottery rules cannot produce " + count
                    + " distinct numbers between " + min + " and " + max);
        }
        Set<Integer> picked = new LinkedHashSet<>();
        while (picked.size() < count) {
            picked.add(random.nextInt(min, max + 1));
        }
        return new ArrayList<>(picked);
    }
}