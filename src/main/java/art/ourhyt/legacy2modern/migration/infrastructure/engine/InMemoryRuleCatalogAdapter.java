package art.ourhyt.legacy2modern.migration.infrastructure.engine;

import art.ourhyt.legacy2modern.migration.application.RuleCatalogPort;
import art.ourhyt.legacy2modern.migration.domain.CobolCommentRule;
import art.ourhyt.legacy2modern.migration.domain.CobolDisplayRule;
import art.ourhyt.legacy2modern.migration.domain.CobolElseRule;
import art.ourhyt.legacy2modern.migration.domain.CobolEndIfRule;
import art.ourhyt.legacy2modern.migration.domain.CobolIfRule;
import art.ourhyt.legacy2modern.migration.domain.CobolMoveRule;
import art.ourhyt.legacy2modern.migration.domain.CobolStopRunRule;
import art.ourhyt.legacy2modern.migration.domain.CobolUnknownLineRule;
import art.ourhyt.legacy2modern.migration.domain.DelphiBeginEndRule;
import art.ourhyt.legacy2modern.migration.domain.DelphiUnknownLineRule;
import art.ourhyt.legacy2modern.migration.domain.Rule;
import art.ourhyt.legacy2modern.migration.domain.SourceLanguage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class InMemoryRuleCatalogAdapter implements RuleCatalogPort {
    @Override
    public List<Rule> orderedRules(SourceLanguage sourceLanguage) {
        if (sourceLanguage == SourceLanguage.DELPHI) {
            return List.of(
                new DelphiBeginEndRule(),
                new DelphiUnknownLineRule()
            );
        }

        return List.of(
            new CobolIfRule(),
            new CobolElseRule(),
            new CobolEndIfRule(),
            new CobolDisplayRule(),
            new CobolMoveRule(),
            new CobolStopRunRule(),
            new CobolCommentRule(),
            new CobolUnknownLineRule()
        );
    }
}
