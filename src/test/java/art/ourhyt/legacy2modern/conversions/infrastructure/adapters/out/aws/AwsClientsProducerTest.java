package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AwsClientsProducerTest {
    @Test
    void createsAwsClients() {
        final AwsClientsProducer producer = new AwsClientsProducer();

        try (var s3 = producer.s3Client();
             var presigner = producer.s3Presigner();
             var dynamo = producer.dynamoDbClient();
             var sqs = producer.sqsClient()) {
            assertNotNull(s3);
            assertNotNull(presigner);
            assertNotNull(dynamo);
            assertNotNull(sqs);
        }
    }
}
