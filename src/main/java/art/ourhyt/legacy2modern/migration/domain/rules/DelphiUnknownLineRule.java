package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;

import java.util.regex.Pattern;

public final class DelphiUnknownLineRule extends SourceSpecificRule {
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*.+;?$");

    @Override
    public String id() {
        return "RULE_DELPHI_UNKNOWN";
    }

    @Override
    public String name() {
        return "Unknown DELPHI line -> comment";
    }

    @Override
    public String description() {
        return "Keeps unsupported Delphi syntax as comments";
    }

    @Override
    protected SourceLanguage sourceLanguage() {
        return SourceLanguage.DELPHI;
    }

    @Override
    protected LineTransform transformLine(String line, MigrationContext context, int lineNumber) {
        final String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.equals("{") || trimmed.equals("}")) {
            return keep(line);
        }
        if (ASSIGNMENT_PATTERN.matcher(trimmed).matches()) {
            return keep(line);
        }
        if (trimmed.startsWith("System.out.println(") || trimmed.startsWith("console.log(") || trimmed.startsWith("print(") || trimmed.startsWith("fmt.Println(")) {
            return keep(line);
        }
        return match("// TODO: UNMAPPED: " + trimmed);
    }
}
