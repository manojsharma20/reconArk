package io.reconark.kernel.pipeline;

import java.io.Serial;

/** A stage failed under {@link ErrorPolicy#FAIL_RUN} or {@link ErrorPolicy#REJECT_RECORD}; the unit of work fails. */
public class PipelineFailure extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String stageKey;

    public PipelineFailure(String stageKey, String message, Throwable cause) {
        super(message, cause);
        this.stageKey = stageKey;
    }

    public String stageKey() {
        return stageKey;
    }
}
