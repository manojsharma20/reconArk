package io.reconark.domain.engine.recon;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A configured pairing of two sources (build prompt §6.6). Business configuration — versioned, maker-checker.
 *
 * @param id rule set code
 * @param leftSource left source id
 * @param rightSource right source id
 * @param matchKey how records pair
 * @param strategy {@code match-strategy} extension key, e.g. {@code one-to-one}
 * @param strategyOptions strategy options
 * @param compareRules ordered compared fields
 */
public record ReconRuleSet(
        String id,
        String leftSource,
        String rightSource,
        MatchKeySpec matchKey,
        String strategy,
        Map<String, Object> strategyOptions,
        List<CompareRule> compareRules) {

    public ReconRuleSet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(matchKey, "matchKey");
        strategy = strategy == null ? "one-to-one" : strategy;
        strategyOptions = strategyOptions == null ? Map.of() : Map.copyOf(strategyOptions);
        compareRules = List.copyOf(compareRules);
    }
}
