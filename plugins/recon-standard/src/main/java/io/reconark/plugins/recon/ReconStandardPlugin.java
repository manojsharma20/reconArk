package io.reconark.plugins.recon;

import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ExtensionPoints;

/** Registers the standard comparators, the one-to-one strategy and the standard classifier. */
public final class ReconStandardPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("recon-standard", "0.2.0")
            .name("Standard recon")
            .description("exact, case-insensitive, numeric-tolerance, date-window, value-map; one-to-one; standard classifier")
            .provides(ExtensionPoints.FIELD_COMPARATOR, ExtensionPoints.MATCH_STRATEGY, ExtensionPoints.OUTCOME_CLASSIFIER)
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar r, PluginContext context) {
        r.contribute(ExtensionPoints.FIELD_COMPARATOR, "exact", new ExactComparator());
        r.contribute(ExtensionPoints.FIELD_COMPARATOR, "case-insensitive", new CaseInsensitiveComparator());
        r.contribute(ExtensionPoints.FIELD_COMPARATOR, "numeric-tolerance", new NumericToleranceComparator());
        r.contribute(ExtensionPoints.FIELD_COMPARATOR, "date-window", new DateWindowComparator());
        r.contribute(ExtensionPoints.FIELD_COMPARATOR, "value-map", new ValueMapComparator());
        r.contribute(ExtensionPoints.MATCH_STRATEGY, "one-to-one", new OneToOneStrategy());
        r.contribute(ExtensionPoints.OUTCOME_CLASSIFIER, "standard", new StandardOutcomeClassifier());
    }
}
