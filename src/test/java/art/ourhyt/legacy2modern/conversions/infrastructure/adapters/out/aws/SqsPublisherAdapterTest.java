package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionMessage;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsPublisherAdapterTest {
    @Test
    void publishesExpectedMessageBody() {
        final SqsClient sqsClient = mock(SqsClient.class);
        final ConversionConfigPort config = new FixedConfig();
        final ObjectMapper mapper = new ObjectMapper();
        final SqsPublisherAdapter adapter = new SqsPublisherAdapter(sqsClient, config, mapper);

        final ConversionMessage message = new ConversionMessage(
            "job-1",
            "bucket",
            "conversions/job-1/request.json",
            "bucket",
            "conversions/job-1/input.cob",
            new ConversionMessage.McpMessage("https://mcp", "convert_code")
        );

        adapter.publish(message);

        final ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertEquals("queue-url", captor.getValue().queueUrl());
    }

    @Test
    void throwsWhenSerializationFails() throws JsonProcessingException {
        final SqsClient sqsClient = mock(SqsClient.class);
        final ConversionConfigPort config = new FixedConfig();
        final ObjectMapper mapper = mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("boom") {
        }).when(mapper).writeValueAsString(org.mockito.ArgumentMatchers.any());

        final SqsPublisherAdapter adapter = new SqsPublisherAdapter(sqsClient, config, mapper);
        final ConversionMessage message = new ConversionMessage("job-1", "b", "r", "b", "c", new ConversionMessage.McpMessage(null, "convert_code"));

        assertThrows(IllegalStateException.class, () -> adapter.publish(message));
    }

    private record FixedConfig() implements ConversionConfigPort {
        @Override
        public String ddbTable() {
            return "table";
        }

        @Override
        public String artifactBucket() {
            return "bucket";
        }

        @Override
        public String queueUrl() {
            return "queue-url";
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
