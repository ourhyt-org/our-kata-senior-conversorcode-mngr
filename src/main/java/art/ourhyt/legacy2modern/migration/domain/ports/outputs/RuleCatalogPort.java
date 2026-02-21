package art.ourhyt.legacy2modern.migration.domain.ports.outputs;

import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.Rule;

import java.util.List;

public interface RuleCatalogPort {
    List<Rule> orderedRules(SourceLanguage sourceLanguage);
}
