package io.reconark.domain.engine.recon;

import io.reconark.domain.model.CanonicalRecord;
import io.reconark.domain.model.MaskingPolicy;
import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.spi.Comparison;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.FieldComparator;
import io.reconark.spi.FieldDiff;
import io.reconark.spi.MatchGroup;
import io.reconark.spi.MatchStrategy;
import io.reconark.spi.OutcomeClassifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The one recon engine. Batch buckets and streaming matches call the same {@link CompiledRuleSet}, which is why they
 * produce identical outcomes. Every variation (strategy, comparators, classification) is an extension.
 */
public final class ReconEngine {

    private final ExtensionLookup extensions;

    public ReconEngine(ExtensionLookup extensions) {
        this.extensions = extensions;
    }

    /** Resolves extensions and validates parameters once per rule-set version. */
    public CompiledRuleSet compile(ReconRuleSet ruleSet) {
        MatchStrategy strategy = extensions.keyed(ExtensionPoints.MATCH_STRATEGY, ruleSet.strategy());
        PluginConfig strategyOptions = new PluginConfig(ruleSet.strategyOptions());
        List<CompiledRule> rules = new ArrayList<>();
        for (CompareRule r : ruleSet.compareRules()) {
            FieldComparator c = extensions.keyed(ExtensionPoints.FIELD_COMPARATOR, r.comparator());
            PluginConfig params = ConfigValidator.validate(ruleSet.id() + "/" + r.leftField(), c.parameters(), r.parameters());
            rules.add(new CompiledRule(r, c, params));
        }
        OutcomeClassifier classifier = extensions.single(ExtensionPoints.OUTCOME_CLASSIFIER);
        return new CompiledRuleSet(ruleSet, strategy, strategyOptions, rules, classifier);
    }

    record CompiledRule(CompareRule rule, FieldComparator comparator, PluginConfig parameters) {}

    /** A rule set ready to evaluate candidates. Thread-safe. */
    public static final class CompiledRuleSet {
        private final ReconRuleSet ruleSet;
        private final MatchStrategy strategy;
        private final PluginConfig strategyOptions;
        private final List<CompiledRule> rules;
        private final OutcomeClassifier classifier;

        CompiledRuleSet(
                ReconRuleSet ruleSet,
                MatchStrategy strategy,
                PluginConfig strategyOptions,
                List<CompiledRule> rules,
                OutcomeClassifier classifier) {
            this.ruleSet = ruleSet;
            this.strategy = strategy;
            this.strategyOptions = strategyOptions;
            this.rules = List.copyOf(rules);
            this.classifier = classifier;
        }

        public ReconRuleSet ruleSet() {
            return ruleSet;
        }

        /** Evaluates one bucket (batch) or one candidate set (streaming). */
        public List<ReconOutcome> reconcile(List<CanonicalRecord> left, List<CanonicalRecord> right) {
            MatchKeySpec mk = ruleSet.matchKey();
            List<MatchGroup> groups = strategy.group(left, right, mk::leftKey, mk::rightKey, strategyOptions);
            List<ReconOutcome> out = new ArrayList<>(groups.size());
            for (MatchGroup g : groups) {
                List<FieldDiff> diffs = g.left().size() == 1 && g.right().size() == 1
                        ? compare(g.left().getFirst(), g.right().getFirst())
                        : List.of();
                out.add(new ReconOutcome(
                        g.key(),
                        classifier.classify(g, diffs),
                        g.left().stream().map(CanonicalRecord::recordKey).toList(),
                        g.right().stream().map(CanonicalRecord::recordKey).toList(),
                        diffs));
            }
            return out;
        }

        private List<FieldDiff> compare(CanonicalRecord l, CanonicalRecord r) {
            List<FieldDiff> diffs = new ArrayList<>();
            for (CompiledRule cr : rules) {
                Object lv = l.fields().get(cr.rule().leftField());
                Object rv = r.fields().get(cr.rule().rightField());
                Comparison c = cr.comparator().compare(lv, rv, cr.parameters());
                if (!c.equal()) {
                    String explanation = cr.rule().sensitive()
                            ? "values differ (" + MaskingPolicy.mask(lv) + " vs " + MaskingPolicy.mask(rv) + ")"
                            : c.explanation();
                    diffs.add(new FieldDiff(cr.rule().leftField(), cr.rule().rightField(), cr.rule().comparator(),
                            cr.rule().severity(), explanation));
                }
            }
            return diffs;
        }
    }
}
