package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.conversions.application.dto.FileContentModel;
import art.ourhyt.legacy2modern.conversions.application.dto.ManifestEntryModel;
import art.ourhyt.legacy2modern.conversions.application.dto.SkippedFileModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipPreviewReader {
    private static final int BUFFER_SIZE = 8192;

    public ZipManifestScan scanManifest(byte[] zipBytes, Set<String> allowedExtensions) {
        final List<ManifestEntryModel> manifest = new ArrayList<>();
        final List<SkippedFileModel> skipped = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String normalizedPath = normalizePath(entry.getName());
                if (!isValidPath(normalizedPath)) {
                    skipped.add(new SkippedFileModel(normalizedPath, "invalid_path"));
                    consumeEntry(zis);
                    continue;
                }
                if (entry.isDirectory()) {
                    skipped.add(new SkippedFileModel(normalizedPath, "directory_entry"));
                    continue;
                }
                final long sizeBytes = consumeEntry(zis);
                final boolean isText = hasSupportedExtension(normalizedPath, allowedExtensions);
                manifest.add(new ManifestEntryModel(normalizedPath, sizeBytes, isText));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read zip content", exception);
        }

        manifest.sort(Comparator.comparing(ManifestEntryModel::path));
        skipped.sort(Comparator.comparing(SkippedFileModel::path));
        return new ZipManifestScan(manifest, skipped);
    }

    public ZipExtractionResult extractSelectedText(
        byte[] zipBytes,
        List<String> requestedPaths,
        int maxFiles,
        int maxTotalBytes,
        int maxFileBytes,
        Set<String> allowedExtensions
    ) {
        final List<FileContentModel> files = new ArrayList<>();
        final List<SkippedFileModel> skipped = new ArrayList<>();
        final Set<String> requested = new HashSet<>(requestedPaths);
        final Set<String> seen = new HashSet<>();
        final Map<String, FileContentModel> collected = new TreeMap<>();
        int totalBytes = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String normalizedPath = normalizePath(entry.getName());
                if (!requested.contains(normalizedPath)) {
                    consumeEntry(zis);
                    continue;
                }
                seen.add(normalizedPath);

                if (!isValidPath(normalizedPath)) {
                    skipped.add(new SkippedFileModel(normalizedPath, "invalid_path"));
                    consumeEntry(zis);
                    continue;
                }
                if (entry.isDirectory()) {
                    skipped.add(new SkippedFileModel(normalizedPath, "directory_entry"));
                    continue;
                }
                if (!hasSupportedExtension(normalizedPath, allowedExtensions)) {
                    skipped.add(new SkippedFileModel(normalizedPath, "unsupported_extension"));
                    consumeEntry(zis);
                    continue;
                }

                final byte[] bytes = readEntryBytes(zis, maxFileBytes + 1);
                if (bytes.length > maxFileBytes) {
                    skipped.add(new SkippedFileModel(normalizedPath, "file_too_large"));
                    continue;
                }
                if (collected.size() >= maxFiles) {
                    skipped.add(new SkippedFileModel(normalizedPath, "total_limit_exceeded"));
                    continue;
                }
                if (totalBytes + bytes.length > maxTotalBytes) {
                    skipped.add(new SkippedFileModel(normalizedPath, "total_limit_exceeded"));
                    continue;
                }

                final String content = decodeUtf8Strict(bytes);
                if (content == null) {
                    skipped.add(new SkippedFileModel(normalizedPath, "binary_or_non_utf8"));
                    continue;
                }

                totalBytes += bytes.length;
                collected.put(normalizedPath, new FileContentModel(normalizedPath, content));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to extract selected zip entries", exception);
        }

        for (String path : requestedPaths) {
            final String normalizedPath = normalizePath(path);
            final FileContentModel content = collected.get(normalizedPath);
            if (content != null) {
                files.add(content);
                continue;
            }
            final boolean hasSkip = skipped.stream().anyMatch(s -> s.path().equals(normalizedPath));
            if (!hasSkip && !seen.contains(normalizedPath)) {
                skipped.add(new SkippedFileModel(normalizedPath, "path_not_found"));
            }
        }

        skipped.sort(Comparator.comparing(SkippedFileModel::path));
        return new ZipExtractionResult(files, skipped);
    }

    private long consumeEntry(ZipInputStream zis) throws IOException {
        long size = 0;
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = zis.read(buffer)) != -1) {
            size += read;
        }
        return size;
    }

    private byte[] readEntryBytes(ZipInputStream zis, int maxBytesToRead) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        int total = 0;
        while ((read = zis.read(buffer)) != -1) {
            if (total + read > maxBytesToRead) {
                final int allowed = maxBytesToRead - total;
                if (allowed > 0) {
                    out.write(buffer, 0, allowed);
                }
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return out.toByteArray();
    }

    private String decodeUtf8Strict(byte[] bytes) {
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            final CharBuffer chars = decoder.decode(ByteBuffer.wrap(bytes));
            return chars.toString();
        } catch (CharacterCodingException exception) {
            return null;
        }
    }

    private boolean hasSupportedExtension(String path, Set<String> allowedExtensions) {
        final String lower = path.toLowerCase();
        return allowedExtensions.stream().anyMatch(lower::endsWith);
    }

    private boolean isValidPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (path.startsWith("/")) {
            return false;
        }
        return !path.matches("(^|/)\\.\\.(?:/|$)");
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    public record ZipManifestScan(List<ManifestEntryModel> manifest, List<SkippedFileModel> skipped) {
    }

    public record ZipExtractionResult(List<FileContentModel> files, List<SkippedFileModel> skipped) {
    }
}
