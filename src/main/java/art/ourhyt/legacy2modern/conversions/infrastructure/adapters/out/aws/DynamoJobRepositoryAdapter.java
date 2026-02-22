package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DynamoJobRepositoryAdapter implements JobRepositoryPort {
    private final DynamoDbClient dynamoDbClient;
    private final ConversionConfigPort config;

    @Inject
    public DynamoJobRepositoryAdapter(DynamoDbClient dynamoDbClient, ConversionConfigPort config) {
        this.dynamoDbClient = dynamoDbClient;
        this.config = config;
    }

    @Override
    public void save(ConversionJob job) {
        final Map<String, AttributeValue> item = new HashMap<>();
        putString(item, "jobId", job.jobId());
        putString(item, "status", job.status() == null ? null : job.status().name());
        putString(item, "createdAt", job.createdAt());
        putString(item, "startedAt", job.startedAt());
        putString(item, "finishedAt", job.finishedAt());
        putString(item, "requestS3Key", job.requestS3Key());
        putString(item, "codeS3Key", job.codeS3Key());
        putString(item, "outputS3Key", job.outputS3Key());
        putString(item, "reportS3Key", job.reportS3Key());
        putString(item, "errorMessage", job.errorMessage());
        if (job.ttl() != null) {
            item.put("ttl", AttributeValue.builder().n(String.valueOf(job.ttl())).build());
        }

        final PutItemRequest request = PutItemRequest.builder()
            .tableName(config.ddbTable())
            .item(item)
            .build();
        dynamoDbClient.putItem(request);
    }

    @Override
    public Optional<ConversionJob> findByJobId(String jobId) {
        final Map<String, AttributeValue> key = Map.of("jobId", AttributeValue.builder().s(jobId).build());
        final GetItemRequest request = GetItemRequest.builder()
            .tableName(config.ddbTable())
            .key(key)
            .build();

        final Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ConversionJob(
            readString(item, "jobId"),
            JobStatus.from(readString(item, "status")),
            readString(item, "createdAt"),
            readString(item, "startedAt"),
            readString(item, "finishedAt"),
            readString(item, "requestS3Key"),
            readString(item, "codeS3Key"),
            readString(item, "outputS3Key"),
            readString(item, "reportS3Key"),
            readString(item, "errorMessage"),
            readLong(item, "ttl")
        ));
    }

    private void putString(Map<String, AttributeValue> item, String key, String value) {
        if (value != null) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private String readString(Map<String, AttributeValue> item, String key) {
        final AttributeValue value = item.get(key);
        return value == null ? null : value.s();
    }

    private Long readLong(Map<String, AttributeValue> item, String key) {
        final AttributeValue value = item.get(key);
        if (value == null || value.n() == null) {
            return null;
        }
        return Long.parseLong(value.n());
    }
}
