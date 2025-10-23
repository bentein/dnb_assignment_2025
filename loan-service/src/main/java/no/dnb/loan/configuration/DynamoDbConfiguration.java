package no.dnb.loan.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@EnableConfigurationProperties(DynamoDbConfiguration.Properties.class)
public class DynamoDbConfiguration {

    private final Properties properties;

    public DynamoDbConfiguration(Properties properties) {
        this.properties = properties;
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    @Data
    @ConfigurationProperties(prefix = "dynamodb")
    public static class Properties {
        private String region = "eu-north-1";
    }
}
