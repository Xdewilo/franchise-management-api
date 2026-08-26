package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stock — value object de existencias")
class StockTest {

    @ParameterizedTest
    @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
    @DisplayName("rechaza cantidades negativas")
    void rejectsNegativeQuantities(long candidate) {
        assertThatThrownBy(() -> new Stock(candidate))
                .isInstanceOf(ValidationException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.INVALID_STOCK);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 5_000L, Long.MAX_VALUE})
    @DisplayName("admite cero y cualquier cantidad positiva")
    void acceptsZeroAndPositiveQuantities(long candidate) {
        assertThat(new Stock(candidate).value()).isEqualTo(candidate);
    }

    @Test
    @DisplayName("interpreta la ausencia de valor como cero")
    void treatsMissingValueAsZero() {
        assertThat(Stock.of(null)).isEqualTo(Stock.ZERO);
    }

    @Test
    @DisplayName("propaga la validación al construir desde un valor opcional")
    void validatesOptionalValue() {
        assertThatThrownBy(() -> Stock.of(-5L)).isInstanceOf(ValidationException.class);
    }
}
