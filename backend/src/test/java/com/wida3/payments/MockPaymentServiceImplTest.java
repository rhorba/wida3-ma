package com.wida3.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.payments.service.MockPaymentServiceImpl;
import com.wida3.payments.service.PaymentResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockPaymentServiceImplTest {

    private final MockPaymentServiceImpl alwaysSucceedGateway = new MockPaymentServiceImpl(true);
    private final MockPaymentServiceImpl alwaysFailGateway = new MockPaymentServiceImpl(false);

    @Test
    void wholeDollarAmount_succeeds() {
        PaymentResult result = alwaysSucceedGateway.charge(new BigDecimal("500.00"), "ref");

        assertThat(result.success()).isTrue();
        assertThat(result.providerRef()).startsWith("MOCK-");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void amountEndingIn13Cents_declinesAsInsufficientFunds() {
        PaymentResult result = alwaysSucceedGateway.charge(new BigDecimal("100.13"), "ref");

        assertThat(result.success()).isFalse();
        assertThat(result.providerRef()).isNull();
        assertThat(result.failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void amountEndingIn66Cents_declinesAsCardDeclined() {
        PaymentResult result = alwaysSucceedGateway.charge(new BigDecimal("200.66"), "ref");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("Card declined by issuer");
    }

    @Test
    void amountEndingIn99Cents_declinesAsGatewayTimeout() {
        PaymentResult result = alwaysSucceedGateway.charge(new BigDecimal("300.99"), "ref");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("Payment gateway timed out, please try again");
    }

    @Test
    void alwaysSucceedFalse_declinesRegardlessOfAmount() {
        PaymentResult result = alwaysFailGateway.charge(new BigDecimal("500.00"), "ref");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("Payment declined by mock gateway");
    }

    @Test
    void refund_alwaysSucceeds() {
        PaymentResult result = alwaysSucceedGateway.refund("MOCK-abc", new BigDecimal("500.00"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerRef()).isEqualTo("MOCK-abc");
    }
}
