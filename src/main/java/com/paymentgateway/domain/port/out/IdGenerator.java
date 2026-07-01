package com.paymentgateway.domain.port.out;

import java.util.UUID;

public interface IdGenerator {
    UUID newId();
}
