package com.wida3.payments.service;

public record PaymentResult(boolean success, String providerRef, String failureReason) {
}
