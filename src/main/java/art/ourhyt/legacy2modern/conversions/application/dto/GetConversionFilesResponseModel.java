package art.ourhyt.legacy2modern.conversions.application.dto;

import java.util.List;

public record GetConversionFilesResponseModel(
    String jobId,
    String status,
    String outputS3Key,
    long zipSizeBytes,
    String defaultFile,
    List<String> recommendedFiles,
    List<ManifestEntryModel> manifest,
    List<FileContentModel> files,
    List<SkippedFileModel> skipped
) {
}
