package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.FileContentModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.ManifestEntryModel;
import art.ourhyt.legacy2modern.conversions.application.dto.SkippedFileModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotReadyException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionFilesInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ObjectStorePort;
import art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws.ZipPreviewReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetConversionFilesService implements GetConversionFilesInputPort {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".go", ".java", ".py", ".ts", ".js", ".json", ".md", ".txt", ".yml", ".yaml", ".properties", ".xml"
    );

    private final JobRepositoryPort jobRepository;
    private final ObjectStorePort objectStore;
    private final ConversionConfigPort config;
    private final ZipPreviewReader zipPreviewReader;

    @Inject
    public GetConversionFilesService(JobRepositoryPort jobRepository, ObjectStorePort objectStore, ConversionConfigPort config) {
        this(jobRepository, objectStore, config, new ZipPreviewReader());
    }

    GetConversionFilesService(JobRepositoryPort jobRepository, ObjectStorePort objectStore, ConversionConfigPort config, ZipPreviewReader zipPreviewReader) {
        this.jobRepository = jobRepository;
        this.objectStore = objectStore;
        this.config = config;
        this.zipPreviewReader = zipPreviewReader;
    }

    @Override
    public GetConversionFilesResponseModel execute(GetConversionFilesRequestModel request) {
        final ConversionJob job = jobRepository.findByJobId(request.jobId())
            .orElseThrow(() -> new ConversionNotFoundException("NOT_FOUND", "Conversion job not found", List.of("jobId=" + request.jobId())));

        final JobStatus status = job.status() == null ? JobStatus.FAILED : job.status();
        if (status != JobStatus.FINISHED) {
            throw new ConversionNotReadyException("NOT_READY", "Conversion output is not ready", List.of("status=" + status.name()));
        }

        if (job.outputS3Key() == null || job.outputS3Key().isBlank()) {
            throw new IllegalStateException("outputS3Key is missing for FINISHED job");
        }

        final int maxFiles = clamp(request.maxFiles(), 30, 100);
        final int maxTotalBytes = clamp(request.maxTotalBytes(), 300000, 1000000);
        final int maxFileBytes = clamp(request.maxFileBytes(), 200000, 300000);

        final byte[] zipBytes = objectStore.getObjectBytes(config.artifactBucket(), job.outputS3Key());
        final ZipPreviewReader.ZipManifestScan manifestScan = zipPreviewReader.scanManifest(zipBytes, ALLOWED_EXTENSIONS);

        final List<ManifestEntryModel> manifest = manifestScan.manifest();
        final List<ManifestEntryModel> safeTextFiles = manifest.stream().filter(ManifestEntryModel::isText).toList();
        final List<String> recommendedFiles = recommendFiles(safeTextFiles);
        final String defaultFile = recommendedFiles.isEmpty() ? null : recommendedFiles.getFirst();

        final List<FileContentModel> files;
        final List<SkippedFileModel> extractedSkipped;
        if (!request.includeContent()) {
            files = List.of();
            extractedSkipped = List.of();
        } else {
            final List<String> selectedPaths = selectPaths(request.paths(), defaultFile);
            final ZipPreviewReader.ZipExtractionResult extraction = zipPreviewReader.extractSelectedText(
                zipBytes,
                selectedPaths,
                maxFiles,
                maxTotalBytes,
                maxFileBytes,
                ALLOWED_EXTENSIONS
            );
            files = extraction.files();
            extractedSkipped = extraction.skipped();
        }

        final List<SkippedFileModel> mergedSkipped = new ArrayList<>();
        mergedSkipped.addAll(manifestScan.skipped());
        mergedSkipped.addAll(extractedSkipped);
        final List<SkippedFileModel> dedupedSkipped = mergedSkipped.stream()
            .collect(Collectors.toMap(
                s -> s.path() + "::" + s.reason(),
                s -> s,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ))
            .values().stream()
            .sorted(Comparator.comparing(SkippedFileModel::path))
            .toList();

        return new GetConversionFilesResponseModel(
            job.jobId(),
            status.name(),
            job.outputS3Key(),
            zipBytes.length,
            defaultFile,
            recommendedFiles,
            manifest,
            files,
            dedupedSkipped
        );
    }

    private List<String> selectPaths(List<String> requestPaths, String defaultFile) {
        if (requestPaths != null && !requestPaths.isEmpty()) {
            return requestPaths.stream().map(this::normalizePath).filter(s -> !s.isBlank()).distinct().toList();
        }
        if (defaultFile == null) {
            return List.of();
        }
        return List.of(defaultFile);
    }

    private List<String> recommendFiles(List<ManifestEntryModel> safeTextFiles) {
        final Set<String> available = safeTextFiles.stream().map(ManifestEntryModel::path).collect(Collectors.toCollection(LinkedHashSet::new));
        final List<String> ranked = new ArrayList<>();

        addIfPresent(ranked, available, "cmd/app/main.go");
        addIfPresent(ranked, available, "main.go");
        addFirstPatternMatch(ranked, available, p -> p.startsWith("src/main/java/") && p.endsWith("/Application.java"));
        addFirstPatternMatch(ranked, available, p -> p.equals("Application.java"));
        addFirstPatternMatch(ranked, available, p -> p.equals("Main.java") || p.endsWith("/Main.java"));
        addIfPresent(ranked, available, "main.py");
        addIfPresent(ranked, available, "src/index.ts");
        addIfPresent(ranked, available, "src/main.ts");
        addIfPresent(ranked, available, "index.js");

        final List<String> sortedFallback = available.stream()
            .filter(path -> !ranked.contains(path))
            .sorted(String::compareTo)
            .toList();

        for (String path : sortedFallback) {
            if (ranked.size() >= 3) {
                break;
            }
            ranked.add(path);
        }

        if (ranked.size() > 3) {
            return ranked.subList(0, 3);
        }
        return ranked;
    }

    private void addIfPresent(List<String> ranked, Set<String> available, String candidate) {
        if (available.contains(candidate) && !ranked.contains(candidate)) {
            ranked.add(candidate);
        }
    }

    private void addFirstPatternMatch(List<String> ranked, Set<String> available, java.util.function.Predicate<String> predicate) {
        available.stream()
            .filter(predicate)
            .sorted(String::compareTo)
            .findFirst()
            .ifPresent(path -> {
                if (!ranked.contains(path)) {
                    ranked.add(path);
                }
            });
    }

    private int clamp(int value, int defaultValue, int maxValue) {
        final int candidate = value <= 0 ? defaultValue : value;
        return Math.min(Math.max(candidate, 1), maxValue);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
