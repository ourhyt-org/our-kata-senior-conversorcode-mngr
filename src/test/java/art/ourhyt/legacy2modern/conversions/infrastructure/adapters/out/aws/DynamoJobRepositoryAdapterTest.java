package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoJobRepositoryAdapterTest {
    @Test
    void savesAndReadsJobs() {
        final DynamoDbClient client = mock(DynamoDbClient.class);
        final DynamoJobRepositoryAdapter adapter = new DynamoJobRepositoryAdapter(client, new FixedConfig());

        final ConversionJob job = new ConversionJob(
            "job-1",
            JobStatus.PENDING,
            "2026-01-01T00:00:00Z",
            null,
            null,
            "req",
            "code",
            null,
            null,
            null,
            123L
        );

        adapter.save(job);

        final ArgumentCaptor<PutItemRequest> putCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(putCaptor.capture());
        assertEquals("table-a", putCaptor.getValue().tableName());
        assertEquals("job-1", putCaptor.getValue().item().get("jobId").s());
        assertEquals("PENDING", putCaptor.getValue().item().get("status").s());

        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(Map.of()).build());
        assertFalse(adapter.findByJobId("missing").isPresent());

        final Map<String, AttributeValue> item = Map.ofEntries(
            Map.entry("jobId", AttributeValue.builder().s("job-2").build()),
            Map.entry("status", AttributeValue.builder().s("FINISHED").build()),
            Map.entry("createdAt", AttributeValue.builder().s("2026-01-01T00:00:00Z").build()),
            Map.entry("startedAt", AttributeValue.builder().s("2026-01-01T00:01:00Z").build()),
            Map.entry("finishedAt", AttributeValue.builder().s("2026-01-01T00:02:00Z").build()),
            Map.entry("requestS3Key", AttributeValue.builder().s("req").build()),
            Map.entry("codeS3Key", AttributeValue.builder().s("code").build()),
            Map.entry("outputS3Key", AttributeValue.builder().s("out").build()),
            Map.entry("reportS3Key", AttributeValue.builder().s("report").build()),
            Map.entry("errorMessage", AttributeValue.builder().s("none").build()),
            Map.entry("ttl", AttributeValue.builder().n("456").build())
        );
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        final ConversionJob loaded = adapter.findByJobId("job-2").orElseThrow();
        assertEquals("job-2", loaded.jobId());
        assertEquals(JobStatus.FINISHED, loaded.status());
        assertEquals("out", loaded.outputS3Key());
        assertEquals(456L, loaded.ttl());

        final ArgumentCaptor<GetItemRequest> getCaptor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(client, org.mockito.Mockito.times(2)).getItem(getCaptor.capture());
        assertEquals("table-a", getCaptor.getAllValues().getFirst().tableName());
        assertTrue(getCaptor.getAllValues().getFirst().key().containsKey("jobId"));
    }

    private record FixedConfig() implements ConversionConfigPort {
        @Override
        public String ddbTable() {
            return "table-a";
        }

        @Override
        public String artifactBucket() {
            return "bucket";
        }

        @Override
        public String queueUrl() {
            return "queue";
        }

        @Override
        public String mcpBaseUrl() {
            return null;
        }

        @Override
        public int presignTtlMinutes() {
            return 15;
        }

        @Override
        public int jobTtlDays() {
            return 7;
        }
    }
}
