package io.reconark.domain.engine.recon;

import io.reconark.domain.model.CanonicalRecord;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * How to build the match key on each side.
 *
 * @param leftFields canonical fields on the left, concatenated in order
 * @param rightFields canonical fields on the right
 * @param normalizations applied to each part, in order
 */
public record MatchKeySpec(List<String> leftFields, List<String> rightFields, List<Normalization> normalizations) {

    public MatchKeySpec {
        leftFields = List.copyOf(leftFields);
        rightFields = List.copyOf(rightFields);
        normalizations = List.copyOf(normalizations == null ? List.of() : normalizations);
        if (leftFields.isEmpty() || leftFields.size() != rightFields.size()) {
            throw new IllegalArgumentException("Match key needs the same, non-zero number of fields on both sides");
        }
    }

    String leftKey(CanonicalRecord r) {
        return key(r, leftFields);
    }

    String rightKey(CanonicalRecord r) {
        return key(r, rightFields);
    }

    private String key(CanonicalRecord r, List<String> fields) {
        return fields.stream()
                .map(f -> normalize(Objects.toString(r.fields().get(f), "")))
                .collect(Collectors.joining("|"));
    }

    private String normalize(String s) {
        String out = s;
        for (Normalization n : normalizations) {
            out = n.apply(out);
        }
        return out;
    }
}
