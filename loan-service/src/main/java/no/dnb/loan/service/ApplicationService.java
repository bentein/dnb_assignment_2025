package no.dnb.loan.service;

import no.dnb.loan.model.application.Application;
import no.dnb.loan.model.application.CreateApplicationRequest;
import no.dnb.loan.model.application.ManageApplicationRequest;
import no.dnb.loan.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public Application createApplication(CreateApplicationRequest request) {
        return applicationRepository.createApplication(request);
    }

    public void manageApplication(UUID id, ManageApplicationRequest request) {
        var status = switch (request.action()) {
            case APPROVE -> Application.Status.APPROVED;
            case REJECT -> Application.Status.REJECTED;
        };
        applicationRepository.setApplicationStatus(id, status);
    }
}
