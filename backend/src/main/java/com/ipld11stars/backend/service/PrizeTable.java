package com.ipld11stars.backend.service;

import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.ContestPrize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds and checks contest prize tables. All amounts are rupees with two decimal places.
 */
public final class PrizeTable {

    private PrizeTable() {
    }

    /**
     * Default table: the pool is shared equally by ranks 1..maxWinners, rounded down to the paisa,
     * with any leftover paise added to rank 1 so the table always adds up to the pool exactly.
     */
    public static List<ContestPrize> defaultFor(BigDecimal prizePool, int maxWinners) {
        List<ContestPrize> prizes = new ArrayList<>();
        if (prizePool == null || prizePool.signum() <= 0 || maxWinners < 1) {
            return prizes;
        }
        BigDecimal pool = prizePool.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal share = pool.divide(BigDecimal.valueOf(maxWinners), 2, RoundingMode.DOWN);
        BigDecimal leftover = pool.subtract(share.multiply(BigDecimal.valueOf(maxWinners)));
        if (leftover.signum() == 0) {
            prizes.add(new ContestPrize(1, maxWinners, share));
            return prizes;
        }
        prizes.add(new ContestPrize(1, 1, share.add(leftover)));
        if (maxWinners > 1 && share.signum() > 0) {
            prizes.add(new ContestPrize(2, maxWinners, share));
        }
        return prizes;
    }

    /**
     * Checks a custom table: contiguous bands starting at rank 1 and ending at maxWinners, positive
     * amounts that never increase down the table, and a total equal to the prize pool.
     */
    public static List<ContestPrize> validate(List<ContestPrize> prizes, BigDecimal prizePool, int maxWinners) {
        if (prizes == null || prizes.isEmpty()) {
            throw new BusinessRuleException("Prize table is empty");
        }
        List<ContestPrize> sorted = new ArrayList<>(prizes);
        sorted.sort(Comparator.comparingInt(ContestPrize::rankFrom));
        int expectedFrom = 1;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal previous = null;
        for (ContestPrize p : sorted) {
            if (p.rankFrom() != expectedFrom || p.rankTo() < p.rankFrom()) {
                throw new BusinessRuleException("Prize bands must be contiguous and start at rank 1");
            }
            if (p.amount() == null || p.amount().signum() <= 0 || p.amount().stripTrailingZeros().scale() > 2) {
                throw new BusinessRuleException("Each prize must be a positive amount with at most two decimal places");
            }
            if (previous != null && p.amount().compareTo(previous) > 0) {
                throw new BusinessRuleException("A lower rank cannot win more than a higher rank");
            }
            total = total.add(p.amount().multiply(BigDecimal.valueOf(p.positions())));
            previous = p.amount();
            expectedFrom = p.rankTo() + 1;
        }
        if (expectedFrom - 1 != maxWinners) {
            throw new BusinessRuleException("Prize bands must cover ranks 1 to " + maxWinners);
        }
        if (total.compareTo(prizePool) != 0) {
            throw new BusinessRuleException("Prize table adds up to " + total.setScale(2) + " but the prize pool is " + prizePool.setScale(2));
        }
        List<ContestPrize> normalized = new ArrayList<>();
        for (ContestPrize p : sorted) {
            normalized.add(new ContestPrize(p.rankFrom(), p.rankTo(), p.amount().setScale(2, RoundingMode.UNNECESSARY)));
        }
        return normalized;
    }

    /** Prize for a 1-based finishing position, or zero outside the table. */
    public static BigDecimal prizeForPosition(List<ContestPrize> prizes, int position) {
        for (ContestPrize p : prizes) {
            if (position >= p.rankFrom() && position <= p.rankTo()) {
                return p.amount();
            }
        }
        return BigDecimal.ZERO.setScale(2);
    }
}
