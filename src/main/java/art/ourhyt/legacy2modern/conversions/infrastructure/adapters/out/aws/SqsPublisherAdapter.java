package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionMessage;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.QueuePublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ApplicationScoped
public class SqsPublisherAdapter implements QueuePublisherPort {
    private final SqsClient sqsClient;
    private final ConversionConfigPort config;
    private final ObjectMapper objectMapper;

    @Inject
    public SqsPublisherAdapter(SqsClient sqsClient, ConversionConfigPort config, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(ConversionMessage message) {
        final String body;
        try {
            body = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize queue message", exception);
        }

        final SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(config.queueUrl())
            .messageBody(body)
            .build();
        sqsClient.sendMessage(request);
    }
}
