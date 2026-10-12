package io.reconark.common.model;

/** One field that did not match: which field, both values and the comparator that decided. */
public record Difference(String field, String leftValue, String rightValue, String comparator) {}
