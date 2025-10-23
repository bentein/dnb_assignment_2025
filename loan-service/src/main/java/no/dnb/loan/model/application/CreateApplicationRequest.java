package no.dnb.loan.model.application;

import jakarta.validation.constraints.NotNull;
import no.dnb.loan.model.SocialSecurityNumber;

import java.math.BigDecimal;

public record CreateApplicationRequest(
        @NotNull SocialSecurityNumber ssn,
        @NotNull String givenName,
        String middleName,
        @NotNull String surname,
        @NotNull BigDecimal equity,
        @NotNull BigDecimal salary,
        @NotNull BigDecimal amount
) {
}
