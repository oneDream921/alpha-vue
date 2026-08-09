package io.github.onedream921.alphavue.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class PaymentSimulationRequests {
    private PaymentSimulationRequests() { }
    public record Create(@NotBlank String channel, @NotNull @Positive Long amountFen, @NotBlank String idempotencyKey) { }
    public record Complete(@NotBlank String status) { }
}
