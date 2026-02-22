package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.config;

import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class ConversionConfigAdapter implements ConversionConfigPort {
    @ConfigProperty(name = "conversions.ddb-table")
    String ddbTable;

    @ConfigProperty(name = "conversions.artifact-bucket")
    String artifactBucket;

    @ConfigProperty(name = "conversions.queue-url")
    String queueUrl;

    @ConfigProperty(name = "conversions.mcp-base-url")
    Optional<String> mcpBaseUrl;

    @ConfigProperty(name = "conversions.presign-ttl-minutes")
    int presignTtlMinutes;

    @ConfigProperty(name = "conversions.job-ttl-days")
    int jobTtlDays;

    @Override
    public String ddbTable() {
        return ddbTable;
    }

    @Override
    public String artifactBucket() {
        return artifactBucket;
    }

    @Override
    public String queueUrl() {
        return queueUrl;
    }

    @Override
    public String mcpBaseUrl() {
        return mcpBaseUrl.orElse(null);
    }

    @Override
    public int presignTtlMinutes() {
        return presignTtlMinutes;
    }

    @Override
    public int jobTtlDays() {
        return jobTtlDays;
    }
}
