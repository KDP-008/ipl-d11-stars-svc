package com.ipld11stars.backend.contest;

import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.service.PrizeTable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrizeTableTest {

    private static ContestPrize p(int from, int to, String amount) {
        return new ContestPrize(from, to, new BigDecimal(amount));
    }

    @Test
    void defaultSplitGivesLeftoverPaiseToRankOne() {
        assertThat(PrizeTable.defaultFor(new BigDecimal("1000.00"), 3))
                .containsExactly(p(1, 1, "333.34"), p(2, 3, "333.33"));
        assertThat(PrizeTable.defaultFor(new BigDecimal("100"), 4)).containsExactly(p(1, 4, "25.00"));
        assertThat(PrizeTable.defaultFor(BigDecimal.ZERO, 3)).isEmpty();
    }

    @Test
    void customTableMustBeContiguousNonIncreasingAndAddUp() {
        BigDecimal pool = new BigDecimal("1000");
        assertThat(PrizeTable.validate(List.of(p(2, 5, "100"), p(1, 1, "600")), pool, 5))
                .containsExactly(p(1, 1, "600.00"), p(2, 5, "100.00"));

        assertThatThrownBy(() -> PrizeTable.validate(List.of(p(1, 1, "600"), p(3, 6, "100")), pool, 6))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("contiguous");
        assertThatThrownBy(() -> PrizeTable.validate(List.of(p(1, 1, "100"), p(2, 2, "900")), pool, 2))
                .hasMessageContaining("lower rank");
        assertThatThrownBy(() -> PrizeTable.validate(List.of(p(1, 1, "600"), p(2, 4, "100")), pool, 4))
                .hasMessageContaining("adds up to 900.00");
        assertThatThrownBy(() -> PrizeTable.validate(List.of(p(1, 1, "1000")), pool, 3))
                .hasMessageContaining("cover ranks 1 to 3");
    }

    @Test
    void prizeForPositionOutsideTableIsZero() {
        List<ContestPrize> table = List.of(p(1, 1, "50"), p(2, 3, "25"));
        assertThat(PrizeTable.prizeForPosition(table, 3)).isEqualByComparingTo("25");
        assertThat(PrizeTable.prizeForPosition(table, 4)).isEqualByComparingTo("0");
    }
}
