package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.UrlSignerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@ApplicationScoped
public class S3PresignedUrlAdapter implements UrlSignerPort {
    private final S3Presigner presigner;
    private final ConversionConfigPort config;

    @Inject
    public S3PresignedUrlAdapter(S3Presigner presigner, ConversionConfigPort config) {
        this.presigner = presigner;
        this.config = config;
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        final GetObjectRequest objectRequest = GetObjectRequest.builder()
            .bucket(config.artifactBucket())
            .key(key)
            .build();
        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(objectRequest)
            .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
