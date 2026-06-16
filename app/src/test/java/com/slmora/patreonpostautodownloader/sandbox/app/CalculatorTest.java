package com.slmora.patreonpostautodownloader.sandbox.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void GivenTwoPositiveNumbers_WhenAdd_ThenReturnsSum() {
        // Arrange + Act
        int result = calculator.add(7, 5);

        // Assert
        assertThat(result).isEqualTo(12);
    }

    @Test
    void GivenRadius_WhenGetAreaOfCircle_ThenReturnsPiRSquared() {
        // Arrange + Act
        double area = calculator.getAreaOfCircle(2.0);

        // Assert
        assertThat(area).isEqualTo(Math.PI * 4.0);
    }

    @Test
    void GivenValidDividendAndDivisor_WhenDivision_ThenReturnsQuotient() {
        // Arrange + Act
        int result = calculator.division(100, 4);

        // Assert
        assertThat(result).isEqualTo(25);
    }

    @Test
    void GivenDivisorZero_WhenDivision_ThenThrowsArithmeticException() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> calculator.division(10, 0))
                .isInstanceOf(ArithmeticException.class);
    }
}

