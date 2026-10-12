package io.reconark.report;

import io.reconark.common.model.ResultRow;
import java.util.List;

/** One page of results. */
public record ResultPage(int page, int size, long total, List<ResultRow> items) {}
