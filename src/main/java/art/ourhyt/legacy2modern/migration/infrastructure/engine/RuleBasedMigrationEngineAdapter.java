package art.ourhyt.legacy2modern.migration.infrastructure.engine;

import art.ourhyt.legacy2modern.migration.application.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.MigrationEnginePort;
import art.ourhyt.legacy2modern.migration.application.RuleCatalogPort;
import art.ourhyt.legacy2modern.migration.domain.AppliedRule;
import art.ourhyt.legacy2modern.migration.domain.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.MigrationReport;
import art.ourhyt.legacy2modern.migration.domain.MigrationResult;
import art.ourhyt.legacy2modern.migration.domain.Rule;
import art.ourhyt.legacy2modern.migration.domain.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.Warning;
import art.ourhyt.legacy2modern.migration.domain.WarningDetector;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class RuleBasedMigrationEngineAdapter implements MigrationEnginePort {
    private final RuleCatalogPort ruleCatalogPort;
    private final WarningDetector warningDetector;

    public RuleBasedMigrationEngineAdapter(RuleCatalogPort ruleCatalogPort) {
        this.ruleCatalogPort = ruleCatalogPort;
        this.warningDetector = new WarningDetector();
    }

    @Override
    public MigrationResult migrate(MigrateRequestModel request) {
        final MigrationContext context = new MigrationContext(request.sourceLanguage(), request.targetLanguage(), request.targetVersion());
        final List<String> originalLines = splitLines(request.code());
        List<String> currentLines = new ArrayList<>(originalLines);
        final List<AppliedRule> appliedRules = new ArrayList<>();

        for (Rule rule : ruleCatalogPort.orderedRules(request.sourceLanguage())) {
            final RuleResult result = rule.apply(currentLines, context);
            currentLines = result.updatedLines();
            if (result.matchesCount() > 0) {
                appliedRules.add(new AppliedRule(rule.id(), rule.name(), rule.description(), result.matchesCount(), result.lineNumbers()));
            }
        }

        final List<Warning> warnings = warningDetector.detect(context, originalLines, currentLines);
        final MigrationReport report = new MigrationReport(appliedRules, warnings);
        return new MigrationResult(String.join("\n", currentLines), report);
    }

    private List<String> splitLines(String code) {
        if (code == null || code.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(code.split("\\R", -1));
    }
}
