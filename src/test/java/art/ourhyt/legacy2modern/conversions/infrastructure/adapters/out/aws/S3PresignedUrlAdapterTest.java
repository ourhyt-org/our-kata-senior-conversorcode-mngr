package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3PresignedUrlAdapterTest {
    @Test
    void createsPresignedGetUrl() throws Exception {
        final S3Presigner presigner = mock(S3Presigner.class);
        final PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/signed"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        final S3PresignedUrlAdapter adapter = new S3PresignedUrlAdapter(presigner, new FixedConfig());
        final String url = adapter.presignGet("conversions/job-1/output.zip", Duration.ofMinutes(15));

        assertEquals("https://example.com/signed", url);
        verify(presigner).presignGetObject(any(GetObjectPresignRequest.class));
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
