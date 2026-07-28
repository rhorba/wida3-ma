package com.wida3.payments.service;

import java.math.BigDecimal;

/**
 * Swappable payment gateway boundary (Architecture ADR-5). {@link MockPaymentServiceImpl}
 * is the only implementation until a real provider is integrated (Epic 5, Story 5.1) --
 * callers must not change when that swap happens.
 */
public interface PaymentService {

    PaymentResult charge(BigDecimal amount, String reference);

    PaymentResult refund(String providerRef, BigDecimal amount);
}
