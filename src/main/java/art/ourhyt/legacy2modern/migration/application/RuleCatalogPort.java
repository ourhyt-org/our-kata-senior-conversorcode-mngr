package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.Rule;
import art.ourhyt.legacy2modern.migration.domain.SourceLanguage;

import java.util.List;

public interface RuleCatalogPort {
    List<Rule> orderedRules(SourceLanguage sourceLanguage);
}
