package com.wida3.payments.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentServiceImpl implements PaymentService {

    private final boolean alwaysSucceed;

    public MockPaymentServiceImpl(@Value("${app.payment.mock.always-succeed:true}") boolean alwaysSucceed) {
        this.alwaysSucceed = alwaysSucceed;
    }

    @Override
    public PaymentResult charge(BigDecimal amount, String reference) {
        if (!alwaysSucceed) {
            return new PaymentResult(false, null, "Payment declined by mock gateway");
        }
        return new PaymentResult(true, "MOCK-" + UUID.randomUUID(), null);
    }

    @Override
    public PaymentResult refund(String providerRef, BigDecimal amount) {
        return new PaymentResult(true, providerRef, null);
    }
}
