package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversionConfigAdapterTest {
    @Test
    void exposesConfiguredValues() {
        final ConversionConfigAdapter adapter = new ConversionConfigAdapter();
        adapter.ddbTable = "table-a";
        adapter.artifactBucket = "bucket-a";
        adapter.queueUrl = "queue-a";
        adapter.mcpBaseUrl = Optional.of("https://mcp");
        adapter.presignTtlMinutes = 17;
        adapter.jobTtlDays = 10;

        assertEquals("table-a", adapter.ddbTable());
        assertEquals("bucket-a", adapter.artifactBucket());
        assertEquals("queue-a", adapter.queueUrl());
        assertEquals("https://mcp", adapter.mcpBaseUrl());
        assertEquals(17, adapter.presignTtlMinutes());
        assertEquals(10, adapter.jobTtlDays());
    }

    @Test
    void returnsNullWhenMcpBaseUrlMissing() {
        final ConversionConfigAdapter adapter = new ConversionConfigAdapter();
        adapter.mcpBaseUrl = Optional.empty();

        assertNull(adapter.mcpBaseUrl());
    }
}
