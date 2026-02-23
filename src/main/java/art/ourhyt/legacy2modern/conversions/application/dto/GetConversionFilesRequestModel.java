package art.ourhyt.legacy2modern.conversions.application.dto;

import java.util.List;

public record GetConversionFilesRequestModel(
    String jobId,
    boolean includeContent,
    List<String> paths,
    int maxFiles,
    int maxTotalBytes,
    int maxFileBytes
) {
}
