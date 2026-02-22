package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;

public final class DelphiBeginEndRule extends SourceSpecificRule {
    @Override
    public String id() {
        return "RULE_DELPHI_BEGIN_END";
    }

    @Override
    public String name() {
        return "DELPHI begin/end -> braces";
    }

    @Override
    public String description() {
        return "Converts Delphi begin/end tokens to braces";
    }

    @Override
    protected SourceLanguage sourceLanguage() {
        return SourceLanguage.DELPHI;
    }

    @Override
    protected LineTransform transformLine(String line, MigrationContext context, int lineNumber) {
        final String trimmed = line.trim();
        if (trimmed.equalsIgnoreCase("begin")) {
            return match("{");
        }
        if (trimmed.equalsIgnoreCase("end") || trimmed.equalsIgnoreCase("end;") || trimmed.equalsIgnoreCase("end.")) {
            return match("}");
        }
        return keep(line);
    }
}
