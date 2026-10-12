package io.reconark.common.model;

import java.util.Map;

/**
 * How to read one side of a reconciliation.
 *
 * @param name          label shown to users, e.g. "Bank statement"
 * @param format        id of the {@link io.reconark.common.plugin.FileFormat} plugin, e.g. {@code csv}
 * @param formatOptions options for that plugin, e.g. {@code delimiter=;}
 * @param fieldMapping  business field name -> column name in the file, e.g. {@code amount -> AMT}. Columns that are
 *                      not mapped keep their own name.
 */
public record SourceSpec(String name, String format, Map<String, String> formatOptions, Map<String, String> fieldMapping) {

    public SourceSpec {
        formatOptions = formatOptions == null ? Map.of() : Map.copyOf(formatOptions);
        fieldMapping = fieldMapping == null ? Map.of() : Map.copyOf(fieldMapping);
    }

    /** Converts a raw row (column -> value) into business fields (field -> value) using {@link #fieldMapping}. */
    public Map<String, String> toBusinessFields(Map<String, String> row) {
        var result = new java.util.LinkedHashMap<>(row);
        fieldMapping.forEach((field, column) -> {
            result.remove(column);
            result.put(field, row.get(column));
        });
        return result;
    }
}
