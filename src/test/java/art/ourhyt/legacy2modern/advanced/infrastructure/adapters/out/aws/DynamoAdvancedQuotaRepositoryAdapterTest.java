package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoAdvancedQuotaRepositoryAdapterTest {
    @Test
    void consumesFirstTry() {
        final DynamoDbClient client = mock(DynamoDbClient.class);
        final DynamoAdvancedQuotaRepositoryAdapter adapter = new DynamoAdvancedQuotaRepositoryAdapter(client, new FixedConfig());

        when(client.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
        when(client.getItem(any(GetItemRequest.class))).thenReturn(
            GetItemResponse.builder().item(Map.of(
                "limit", AttributeValue.builder().n("10").build(),
                "count", AttributeValue.builder().n("1").build()
            )).build()
        );

        final ConsumeQuotaResult result = adapter.consumeDaily("user-1", 10);

        assertTrue(result.allowed());
        assertEquals(10, result.quotaStatus().limit());
        assertEquals(1, result.quotaStatus().used());
        assertEquals(9, result.quotaStatus().remaining());
        verify(client).getItem(any(GetItemRequest.class));
    }

    @Test
    void returnsExceededWhenConditionFails() {
        final DynamoDbClient client = mock(DynamoDbClient.class);
        final DynamoAdvancedQuotaRepositoryAdapter adapter = new DynamoAdvancedQuotaRepositoryAdapter(client, new FixedConfig());

        when(client.updateItem(any(UpdateItemRequest.class)))
            .thenReturn(UpdateItemResponse.builder().build())
            .thenThrow(ConditionalCheckFailedException.builder().build());
        when(client.getItem(any(GetItemRequest.class))).thenReturn(
            GetItemResponse.builder().item(Map.of(
                "limit", AttributeValue.builder().n("10").build(),
                "count", AttributeValue.builder().n("10").build()
            )).build()
        );

        final ConsumeQuotaResult result = adapter.consumeDaily("user-1", 10);

        assertFalse(result.allowed());
        assertEquals(10, result.quotaStatus().used());
        assertEquals(0, result.quotaStatus().remaining());
    }

    @Test
    void getTodayReturnsDefaultsWhenMissing() {
        final DynamoDbClient client = mock(DynamoDbClient.class);
        final DynamoAdvancedQuotaRepositoryAdapter adapter = new DynamoAdvancedQuotaRepositoryAdapter(client, new FixedConfig());

        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(Map.of()).build());

        final QuotaStatus status = adapter.getToday("user-1", 10);
        assertEquals(10, status.limit());
        assertEquals(0, status.used());
        assertEquals(10, status.remaining());
    }

    private record FixedConfig() implements AdvancedConfigPort {
        @Override
        public String usageTable() {
            return "kata-advanced-usage-qa";
        }

        @Override
        public int defaultDailyLimit() {
            return 10;
        }

        @Override
        public String supabaseJwksUrl() {
            return "https://example.supabase.co/auth/v1/.well-known/jwks.json";
        }

        @Override
        public String supabaseIssuer() {
            return "https://example.supabase.co/auth/v1";
        }

        @Override
        public String supabaseAudience() {
            return "";
        }
    }
}
