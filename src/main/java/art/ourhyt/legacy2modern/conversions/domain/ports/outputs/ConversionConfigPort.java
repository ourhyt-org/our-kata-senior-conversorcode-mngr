package art.ourhyt.legacy2modern.conversions.domain.ports.outputs;

public interface ConversionConfigPort {
    String ddbTable();

    String artifactBucket();

    String queueUrl();

    String mcpBaseUrl();

    int presignTtlMinutes();

    int jobTtlDays();
}
