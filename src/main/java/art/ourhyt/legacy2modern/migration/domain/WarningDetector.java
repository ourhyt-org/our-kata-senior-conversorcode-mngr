package art.ourhyt.legacy2modern.migration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WarningDetector {
    private static final List<String> COBOL_UNSUPPORTED = List.of("ACCEPT", "PERFORM", "EVALUATE", "COMPUTE");

    public List<Warning> detect(MigrationContext context, List<String> inputLines, List<String> outputLines) {
        final List<Warning> warnings = new ArrayList<>();
        if (inputLines.isEmpty() || inputLines.stream().allMatch(line -> line.trim().isEmpty())) {
            warnings.add(new Warning("WARN_EMPTY_INPUT", "Input code is empty", List.of()));
            return warnings;
        }

        if (context.sourceLanguage() == SourceLanguage.COBOL) {
            warnings.addAll(detectUnsupportedCobol(inputLines));
            warnings.addAll(detectMissingEndIf(inputLines));
        }

        warnings.addAll(detectUnknownSyntax(outputLines));

        if (context.sourceLanguage() == SourceLanguage.DELPHI) {
            warnings.add(new Warning("WARN_DELPHI_LIMITED", "DELPHI migration limited", List.of()));
        }

        return warnings;
    }

    private List<Warning> detectUnsupportedCobol(List<String> inputLines) {
        final List<Warning> warnings = new ArrayList<>();
        for (int i = 0; i < inputLines.size(); i++) {
            final String normalized = inputLines.get(i).trim().toUpperCase(Locale.ROOT);
            for (String keyword : COBOL_UNSUPPORTED) {
                if (normalized.startsWith(keyword + " ") || normalized.equals(keyword)) {
                    warnings.add(new Warning("WARN_UNSUPPORTED", "Unsupported keyword: " + keyword, List.of(i + 1)));
                }
            }
        }
        return warnings;
    }

    private List<Warning> detectMissingEndIf(List<String> inputLines) {
        int ifCount = 0;
        int endIfCount = 0;
        for (String line : inputLines) {
            final String normalized = line.trim().toUpperCase(Locale.ROOT);
            if (normalized.matches("^IF\\s+.+\\s+THEN$")) {
                ifCount++;
            }
            if (normalized.equals("END-IF")) {
                endIfCount++;
            }
        }
        if (ifCount > endIfCount) {
            return List.of(new Warning("WARN_MISSING_END_IF", "Possible missing END-IF", List.of()));
        }
        return List.of();
    }

    private List<Warning> detectUnknownSyntax(List<String> outputLines) {
        final List<Warning> warnings = new ArrayList<>();
        for (int i = 0; i < outputLines.size(); i++) {
            if (outputLines.get(i).trim().startsWith("// TODO: UNMAPPED:")) {
                warnings.add(new Warning("WARN_UNKNOWN_SYNTAX", "Unknown syntax kept as comment", List.of(i + 1)));
            }
        }
        return warnings;
    }
}
