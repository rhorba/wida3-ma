package com.wida3.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Deterministic mock gateway (Architecture ADR-5). When app.payment.mock.always-succeed is true
 * (default), every charge succeeds except for a few reserved total-price cent values that
 * simulate realistic decline scenarios for testing/demo -- see the mapping table next to ADR-5
 * in docs/architecture-wida3-ma.md. There is no card-entry field anywhere in the app, so the
 * total price is the only per-request signal available without adding new API surface.
 */
@Service
public class MockPaymentServiceImpl implements PaymentService {

    private static final int INSUFFICIENT_FUNDS_CENTS = 13;
    private static final int CARD_DECLINED_CENTS = 66;
    private static final int GATEWAY_TIMEOUT_CENTS = 99;
    private static final long SIMULATED_TIMEOUT_DELAY_MS = 300;

    private final boolean alwaysSucceed;

    public MockPaymentServiceImpl(@Value("${app.payment.mock.always-succeed:true}") boolean alwaysSucceed) {
        this.alwaysSucceed = alwaysSucceed;
    }

    @Override
    public PaymentResult charge(BigDecimal amount, String reference) {
        if (!alwaysSucceed) {
            return new PaymentResult(false, null, "Payment declined by mock gateway");
        }

        int cents = fractionalCents(amount);
        return switch (cents) {
            case INSUFFICIENT_FUNDS_CENTS -> new PaymentResult(false, null, "Insufficient funds");
            case CARD_DECLINED_CENTS -> new PaymentResult(false, null, "Card declined by issuer");
            case GATEWAY_TIMEOUT_CENTS -> {
                simulateDelay();
                yield new PaymentResult(false, null, "Payment gateway timed out, please try again");
            }
            default -> new PaymentResult(true, "MOCK-" + UUID.randomUUID(), null);
        };
    }

    @Override
    public PaymentResult refund(String providerRef, BigDecimal amount) {
        return new PaymentResult(true, providerRef, null);
    }

    private int fractionalCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .remainder(BigDecimal.ONE)
                .abs()
                .movePointRight(2)
                .intValue();
    }

    private void simulateDelay() {
        try {
            Thread.sleep(SIMULATED_TIMEOUT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
