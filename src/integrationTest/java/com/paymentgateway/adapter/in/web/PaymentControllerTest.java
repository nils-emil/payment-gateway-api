package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.support.exception.*;
import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.usecase.getpayment.GetPaymentUseCase;
import com.paymentgateway.domain.usecase.processpayment.ProcessPaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({PaymentWebMapper.class, PaymentExceptionHandler.class})
class PaymentControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean ProcessPaymentUseCase processPaymentUseCase;
    @MockitoBean GetPaymentUseCase getPaymentUseCase;

    private final String validBody = """
        {"card_number":"2222405343248877","expiry_month":4,"expiry_year":2027,
         "currency":"GBP","amount":100,"cvv":"123"}
        """;

    @Test
    void postReturns200AndAuthorizedBody() throws Exception {
        Payment authorized = Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(100, "GBP"))
                .authorize("auth-1");
        when(processPaymentUseCase.process(any())).thenReturn(authorized);

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Authorized"))
                .andExpect(jsonPath("$.last_four").value("8877"))
                .andExpect(jsonPath("$.authorization_code").value("auth-1"))
                .andExpect(jsonPath("$.cvv").doesNotExist())
                .andExpect(jsonPath("$.card_number").doesNotExist());
    }

    @Test
    void validationExceptionReturns400RejectedWithErrors() throws Exception {
        when(processPaymentUseCase.process(any()))
                .thenThrow(new ValidationException(List.of(new ValidationError("cvv.invalid", "cvv must be 3-4 digits"))));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.errors[0].code").value("cvv.invalid"))
                .andExpect(jsonPath("$.errors[0].description").value("cvv must be 3-4 digits"));
    }

    @Test
    void bankUnavailableReturns502() throws Exception {
        when(processPaymentUseCase.process(any()))
                .thenThrow(new BankUnavailableException("down", null));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("acquiring bank unavailable"))
                .andExpect(jsonPath("$.payment_id").doesNotExist());
    }

    @Test
    void inDoubtPendingReplayReturns409() throws Exception {
        when(processPaymentUseCase.process(any()))
                .thenThrow(new PaymentInProgressException(UUID.randomUUID()));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("payment is still being processed"));
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("malformed request body"));
    }

    @Test
    void unexpectedErrorReturns500() throws Exception {
        when(processPaymentUseCase.process(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal error"));
    }

    @Test
    void getReturns200ForKnownPayment() throws Exception {
        Payment p = Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(100, "GBP"));
        when(getPaymentUseCase.getById(p.id())).thenReturn(p);

        mvc.perform(get("/payments/{id}", p.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"));
    }

    @Test
    void getReturns404ForUnknownPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(getPaymentUseCase.getById(id)).thenThrow(new EntityNotFoundException("payment", id));

        mvc.perform(get("/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("payment not found"));
    }

    @Test
    void getWithMalformedIdReturns400() throws Exception {
        mvc.perform(get("/payments/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("malformed payment id"));
    }
}
