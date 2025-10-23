package no.dnb.loan.repository;

import lombok.extern.slf4j.Slf4j;
import no.dnb.loan.model.application.Application;
import no.dnb.loan.model.application.CreateApplicationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Repository
public class ApplicationRepository {

    private final DynamoDbClient dynamoDbClient;

    public ApplicationRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public Application createApplication(CreateApplicationRequest create) {
        var id = UUID.randomUUID();
        var item = new HashMap<>(Map.of(
                "id", value(id.toString()),
                "ssn", value(create.ssn().value()),
                "givenName", value(create.givenName()),
                "surname", value(create.surname()),
                "equity", value(create.equity()),
                "salary", value(create.salary()),
                "amount", value(create.amount()),
                "status", value(Application.Status.PENDING)
        ));
        if (create.middleName() != null) {
            item.put("middleName", value(create.middleName()));
        }
        var request = PutItemRequest.builder()
                .tableName("loan-applications")
                .item(item)
                .build();
        try {
            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            log.error("Could not create application: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create loan application");
        }
        return new Application(id);
    }

    public void setApplicationStatus(UUID id, Application.Status status) {
        var request = UpdateItemRequest.builder()
                .tableName("loan-applications")
                .key(Map.of("id", value(id.toString())))
                .updateExpression("SET #s = :status")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of( ":status", value(status)))
                .build();
        try {
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            log.error("Could not update application: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create loan application");
        }
    }

    private static AttributeValue value(Object obj) {
        if (obj == null) {
            return AttributeValue.fromNul(true);
        }
        return switch (obj) {
            case String s -> AttributeValue.fromS(s);
            case BigDecimal bd -> AttributeValue.fromN(bd.toString());
            case Enum<?> en -> AttributeValue.fromS(en.toString());
            default -> throw new IllegalArgumentException("This should never happen.");
        };
    }
}
