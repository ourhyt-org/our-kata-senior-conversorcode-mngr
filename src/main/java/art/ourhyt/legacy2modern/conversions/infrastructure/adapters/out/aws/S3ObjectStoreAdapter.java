package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ObjectStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ApplicationScoped
public class S3ObjectStoreAdapter implements ObjectStorePort {
    private final S3Client s3Client;
    private final ConversionConfigPort config;
    private final ObjectMapper objectMapper;

    @Inject
    public S3ObjectStoreAdapter(S3Client s3Client, ConversionConfigPort config, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public void putJson(String key, Map<String, Object> payload) {
        try {
            final byte[] bytes = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            putObject(key, "application/json", bytes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize request payload", exception);
        }
    }

    @Override
    public void putText(String key, String payload) {
        putObject(key, "text/plain", payload.getBytes(StandardCharsets.UTF_8));
    }

    private void putObject(String key, String contentType, byte[] bytes) {
        final PutObjectRequest request = PutObjectRequest.builder()
            .bucket(config.artifactBucket())
            .key(key)
            .contentType(contentType)
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }
}
