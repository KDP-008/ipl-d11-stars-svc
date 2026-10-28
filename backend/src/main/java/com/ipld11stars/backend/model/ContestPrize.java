package com.ipld11stars.backend.model;

import java.math.BigDecimal;

/**
 * Prize paid to each position from rankFrom to rankTo (inclusive).
 */
public record ContestPrize(int rankFrom, int rankTo, BigDecimal amount) {

    public int positions() {
        return rankTo - rankFrom + 1;
    }
}
