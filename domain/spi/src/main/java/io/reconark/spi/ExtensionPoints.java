package io.reconark.spi;

import static io.reconark.kernel.api.Cardinality.CHAIN;
import static io.reconark.kernel.api.Cardinality.KEYED;
import static io.reconark.kernel.api.Cardinality.SINGLE;

import io.reconark.kernel.api.ExtensionPoint;
import io.reconark.kernel.pipeline.PipelineStage;
import java.util.List;

/** The catalogue of extension points (HLD §7). Adding a socket is an ADR-level decision. */
public final class ExtensionPoints {

    // --- platform (SINGLE: exactly one active per service)
    public static final ExtensionPoint<MessageBus> MESSAGE_BUS = ExtensionPoint.of("message-bus", MessageBus.class, SINGLE);
    public static final ExtensionPoint<SecretProvider> SECRET_PROVIDER =
            ExtensionPoint.of("secret-provider", SecretProvider.class, SINGLE);
    public static final ExtensionPoint<ObjectStore> OBJECT_STORE = ExtensionPoint.of("object-store", ObjectStore.class, SINGLE);

    // --- ETL (KEYED: selected by provider configuration)
    public static final ExtensionPoint<SourceConnector> SOURCE_CONNECTOR =
            ExtensionPoint.of("source-connector", SourceConnector.class, KEYED);
    public static final ExtensionPoint<PayloadDecoder> PAYLOAD_DECODER =
            ExtensionPoint.of("payload-decoder", PayloadDecoder.class, KEYED);
    public static final ExtensionPoint<FormatReader> FORMAT_READER = ExtensionPoint.of("format-reader", FormatReader.class, KEYED);
    public static final ExtensionPoint<RecordValidator> RECORD_VALIDATOR =
            ExtensionPoint.of("record-validator", RecordValidator.class, KEYED);
    public static final ExtensionPoint<Enricher> ENRICHER = ExtensionPoint.of("enricher", Enricher.class, KEYED);
    public static final ExtensionPoint<PipelineStage> PIPELINE_STAGE = PipelineStage.POINT;

    // --- recon
    public static final ExtensionPoint<MatchStrategy> MATCH_STRATEGY = ExtensionPoint.of("match-strategy", MatchStrategy.class, KEYED);
    public static final ExtensionPoint<FieldComparator> FIELD_COMPARATOR =
            ExtensionPoint.of("field-comparator", FieldComparator.class, KEYED);
    public static final ExtensionPoint<OutcomeClassifier> OUTCOME_CLASSIFIER =
            ExtensionPoint.of("outcome-classifier", OutcomeClassifier.class, SINGLE);

    // --- reporting and notification
    public static final ExtensionPoint<ReportRenderer> REPORT_RENDERER =
            ExtensionPoint.of("report-renderer", ReportRenderer.class, KEYED);
    public static final ExtensionPoint<Notifier> NOTIFIER = ExtensionPoint.of("notifier", Notifier.class, KEYED);

    // --- cross-cutting (CHAIN: ordered)
    public static final ExtensionPoint<AuditSink> AUDIT_SINK = ExtensionPoint.of("audit-sink", AuditSink.class, CHAIN);

    /** Every extension point, for catalogues and documentation. */
    public static final List<ExtensionPoint<?>> ALL = List.of(
            MESSAGE_BUS, SECRET_PROVIDER, OBJECT_STORE,
            SOURCE_CONNECTOR, PAYLOAD_DECODER, FORMAT_READER, RECORD_VALIDATOR, ENRICHER, PIPELINE_STAGE,
            MATCH_STRATEGY, FIELD_COMPARATOR, OUTCOME_CLASSIFIER,
            REPORT_RENDERER, NOTIFIER, AUDIT_SINK);

    private ExtensionPoints() {}
}
