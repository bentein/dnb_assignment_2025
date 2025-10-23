package no.dnb.loan.model.application;

import org.springframework.lang.NonNull;

public record ManageApplicationRequest(@NonNull Action action) {
    public enum Action {
        APPROVE,
        REJECT
    }
}
