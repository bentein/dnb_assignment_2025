package no.dnb.loan.model.application;

import java.util.UUID;

public record Application(UUID id) {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
