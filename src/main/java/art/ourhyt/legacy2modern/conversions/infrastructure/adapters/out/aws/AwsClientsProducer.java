package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class AwsClientsProducer {
    @Produces
    @ApplicationScoped
    public S3Client s3Client() {
        return S3Client.builder().region(resolveRegion()).build();
    }

    @Produces
    @ApplicationScoped
    public S3Presigner s3Presigner() {
        return S3Presigner.builder().region(resolveRegion()).build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder().region(resolveRegion()).build();
    }

    @Produces
    @ApplicationScoped
    public SqsClient sqsClient() {
        return SqsClient.builder().region(resolveRegion()).build();
    }

    private Region resolveRegion() {
        try {
            return new DefaultAwsRegionProviderChain().getRegion();
        } catch (SdkClientException exception) {
            return Region.US_EAST_1;
        }
    }
}
