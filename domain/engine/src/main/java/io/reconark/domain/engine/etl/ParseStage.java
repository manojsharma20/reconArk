package io.reconark.domain.engine.etl;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.pipeline.PipelineStage;
import io.reconark.kernel.pipeline.RecordEnvelope;
import io.reconark.kernel.pipeline.StageContext;
import io.reconark.kernel.pipeline.StagePayload;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.FormatReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code parse}: bytes to records through the {@code format-reader} named in the options. */
final class ParseStage implements PipelineStage {

    @Override
    public ConfigSpec options() {
        return ConfigSpec.of(
                PropertySpec.required("format", PropertyType.STRING, "format-reader key, e.g. delimited"),
                PropertySpec.optional("format-options", PropertyType.MAP, Map.of(), "options passed to the format reader"));
    }

    @Override
    public StagePayload apply(StageContext context, StagePayload input) throws Exception {
        if (!(input instanceof StagePayload.Bytes(var source))) {
            throw new IllegalStateException("parse expects bytes; it must come before record stages");
        }
        FormatReader reader = context.extensions().keyed(ExtensionPoints.FORMAT_READER, context.options().string("format"));
        PluginConfig readerOptions = ConfigValidator.validate(
                "format:" + context.options().string("format"), reader.options(), context.options().map("format-options"));
        List<RecordEnvelope> records = new ArrayList<>();
        try (InputStream in = source.get()) {
            reader.read(in, readerOptions, (position, raw) -> records.add(new RecordEnvelope(position, raw)));
        }
        return new StagePayload.Records(records);
    }
}
