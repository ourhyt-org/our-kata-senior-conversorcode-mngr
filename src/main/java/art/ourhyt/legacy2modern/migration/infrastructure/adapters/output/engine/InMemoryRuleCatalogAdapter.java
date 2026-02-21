package art.ourhyt.legacy2modern.migration.infrastructure.adapters.output.engine;

import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.ports.output.RuleCatalogPort;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolCommentRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolDisplayRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolElseRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolEndIfRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolIfRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolMoveRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolStopRunRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolUnknownLineRule;
import art.ourhyt.legacy2modern.migration.domain.rules.DelphiBeginEndRule;
import art.ourhyt.legacy2modern.migration.domain.rules.DelphiUnknownLineRule;
import art.ourhyt.legacy2modern.migration.domain.rules.Rule;
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
