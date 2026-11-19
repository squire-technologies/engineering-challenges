package com.squire.salon.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * @param staffId leave out to book any available staff member
 * @param start ISO 8601 instant, for example 2026-12-01T15:00:00.000Z
 */
public record BookAppointmentRequest(
    @NotNull UUID shopId,
    @NotNull UUID serviceId,
    UUID staffId,
    @NotNull Instant start,
    @NotBlank String customerName,
    @NotBlank String customerPhone) {}
