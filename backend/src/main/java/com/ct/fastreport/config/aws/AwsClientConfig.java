package com.ct.fastreport.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
@Profile("aws")
public class AwsClientConfig {

    @Bean
    public Region awsRegion(@Value("${aws.region:${AWS_REGION:us-east-1}}") String region) {
        return Region.of(region);
    }

    @Bean
    public SqsClient sqsClient(Region awsRegion) {
        return SqsClient.builder()
                .region(awsRegion)
                .build();
    }

    @Bean
    public ApiGatewayManagementApiClient apiGatewayManagementApiClient(
            Region awsRegion,
            @Value("${aws.websocket.management-endpoint:${WEBSOCKET_API_ENDPOINT:}}") String endpoint
    ) {
        ApiGatewayManagementApiClientBuilder builder = ApiGatewayManagementApiClient.builder()
                .region(awsRegion);
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
