package com.paymentgateway.domain.model;

public record Card(String number, String cvv, ExpiryDate expiry) {
    public Card {
        if (number == null || !number.matches("\\d{14,19}")) {
            throw new ValidationException("card.number.invalid", "card number must be 14-19 digits");
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new ValidationException("cvv.invalid", "cvv must be 3-4 digits");
        }
    }

    public static Card of(String number, String cvv, ExpiryDate expiry) {
        return new Card(number, cvv, expiry);
    }

    public String lastFour() {
        return number.substring(number.length() - 4);
    }

    @Override
    public String toString() {
        return "Card[lastFour=" + lastFour() + "]";
    }
}
