package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ObjectStoreAdapterTest {
    @Test
    void putsJsonAndTextAndReadsBytes() {
        final S3Client s3Client = mock(S3Client.class);
        final S3ObjectStoreAdapter adapter = new S3ObjectStoreAdapter(s3Client, new FixedConfig(), new ObjectMapper());

        adapter.putJson("k1", Map.of("a", "b"));
        adapter.putText("k2", "hello");

        final ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, org.mockito.Mockito.times(2)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals("bucket-a", putCaptor.getAllValues().getFirst().bucket());
        assertEquals("k1", putCaptor.getAllValues().getFirst().key());
        assertEquals("application/json", putCaptor.getAllValues().getFirst().contentType());
        assertEquals("k2", putCaptor.getAllValues().get(1).key());
        assertEquals("text/plain", putCaptor.getAllValues().get(1).contentType());

        final byte[] expected = "abc".getBytes();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), expected));

        final byte[] actual = adapter.getObjectBytes("bucket-a", "k3");
        assertArrayEquals(expected, actual);

        final ArgumentCaptor<GetObjectRequest> getCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(getCaptor.capture());
        assertEquals("bucket-a", getCaptor.getValue().bucket());
        assertEquals("k3", getCaptor.getValue().key());
    }

    private record FixedConfig() implements ConversionConfigPort {
        @Override
        public String ddbTable() {
            return "table";
        }

        @Override
        public String artifactBucket() {
            return "bucket-a";
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
