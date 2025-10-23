package no.dnb.loan.controller;

import jakarta.validation.Valid;
import no.dnb.loan.model.application.CreateApplicationRequest;
import no.dnb.loan.model.application.CreateApplicationResponse;
import no.dnb.loan.model.application.ManageApplicationRequest;
import no.dnb.loan.service.ApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/loan-applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(consumes = "application/json", produces = "application/json")
    public CreateApplicationResponse createApplication(
            @RequestBody @Valid CreateApplicationRequest request
    ) {
        var application = applicationService.createApplication(request);
        return new CreateApplicationResponse(application.id());
    }

    @PreAuthorize("hasRole('ADVISOR')")
    @PostMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public void manageApplication(
            @PathVariable UUID id,
            @RequestBody @Valid ManageApplicationRequest request
    ) {
        applicationService.manageApplication(id, request);
    }
}
