package com.paymentgateway.domain.model;

public enum PaymentStatus {
    PENDING("Pending"),
    AUTHORIZED("Authorized"),
    DECLINED("Declined");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
