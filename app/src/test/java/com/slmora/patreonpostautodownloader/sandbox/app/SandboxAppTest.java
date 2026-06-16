package com.slmora.patreonpostautodownloader.sandbox.app;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SandboxAppTest {

    @Test
    void GivenSandboxApp_WhenGetGreeting_ThenReturnsHelloWorld() {
        // Arrange
        App app = new App();

        // Act
        String greeting = app.getGreeting();

        // Assert
        assertThat(greeting).isEqualTo("Hello World!");
    }

    @Test
    void GivenCalculatorDivisionSucceeds_WhenMainInvoked_ThenDivisionIsCalled() {
        try (MockedConstruction<Calculator> calculatorConstruction =
                     org.mockito.Mockito.mockConstruction(Calculator.class,
                             (mock, context) -> org.mockito.Mockito.when(mock.division(100, 2)).thenReturn(50))) {
            // Act
            App.main(new String[0]);

            // Assert
            Calculator calculator = calculatorConstruction.constructed().getFirst();
            verify(calculator, times(1)).division(100, 2);
        }
    }

    @Test
    void GivenCalculatorDivisionThrows_WhenMainInvoked_ThenExceptionPathIsHandled() {
        try (MockedConstruction<Calculator> calculatorConstruction =
                     org.mockito.Mockito.mockConstruction(Calculator.class,
                             (mock, context) -> doThrow(new ArithmeticException("/ by zero")).when(mock).division(100, 2))) {
            // Act
            App.main(new String[0]);

            // Assert
            Calculator calculator = calculatorConstruction.constructed().getFirst();
            verify(calculator, times(1)).division(100, 2);
        }
    }
}

