package com.paymentgateway.domain.support.util;

public final class Sensitive {

    private Sensitive() {
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + lastFour(cardNumber);
    }

    public static String lastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "";
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
