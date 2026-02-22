package art.ourhyt.legacy2modern.conversions.domain.model;

public record ConversionMessage(
    String jobId,
    String requestS3Bucket,
    String requestS3Key,
    String codeS3Bucket,
    String codeS3Key,
    McpMessage mcp
) {
    public record McpMessage(String baseUrl, String tool) {
    }
}
