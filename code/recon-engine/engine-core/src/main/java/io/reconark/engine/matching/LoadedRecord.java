package io.reconark.engine.matching;

import io.reconark.common.model.Side;
import java.util.Map;

/** A record read from a file and stored by a LOAD job; the input of matching. */
public record LoadedRecord(Side side, String matchKey, long lineNumber, Map<String, String> fields) {}
