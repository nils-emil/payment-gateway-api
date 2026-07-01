package com.paymentgateway.adapter.out.id;

import com.paymentgateway.domain.port.out.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomIdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
