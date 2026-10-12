package io.reconark.common.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads a file into rows. One plugin per file format (CSV, fixed-width, JSON lines, ISO 20022 ...).
 *
 * <p>Implementations must stream: never load the whole file into memory, files can hold millions of rows.
 */
public interface FileFormat extends Plugin {

    /**
     * Reads every row and hands it to {@code onRow} as column name -> value.
     *
     * @param content the file content; the caller closes it
     * @param options format options from the {@link io.reconark.common.model.SourceSpec}
     * @param onRow   called once per data row, in file order
     * @throws IOException              when the file cannot be read
     * @throws IllegalArgumentException when the file or the options are invalid (the message names the line)
     */
    void read(InputStream content, Map<String, String> options, Consumer<Map<String, String>> onRow) throws IOException;
}
