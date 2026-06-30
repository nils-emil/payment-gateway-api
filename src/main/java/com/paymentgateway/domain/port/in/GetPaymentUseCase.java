package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.model.Payment;
import java.util.UUID;

public interface GetPaymentUseCase {
    Payment getById(UUID id);
}
