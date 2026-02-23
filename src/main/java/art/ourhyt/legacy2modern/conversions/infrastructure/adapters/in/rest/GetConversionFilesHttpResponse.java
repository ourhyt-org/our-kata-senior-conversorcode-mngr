package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import java.util.List;

public record GetConversionFilesHttpResponse(
    String jobId,
    String status,
    String outputS3Key,
    long zipSizeBytes,
    String defaultFile,
    List<String> recommendedFiles,
    List<ManifestEntryHttpResponse> manifest,
    List<FileContentHttpResponse> files,
    List<SkippedFileHttpResponse> skipped
) {
}
