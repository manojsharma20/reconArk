/**
 * Configuration-driven pipelines. A {@link io.reconark.kernel.pipeline.PipelineDefinition} lists stage keys; the
 * {@link io.reconark.kernel.pipeline.PipelineEngine} resolves each key to a
 * {@link io.reconark.kernel.pipeline.PipelineStage} extension and enforces the record-accounting invariant
 * {@code read = accepted + rejected + quarantined}.
 */
package io.reconark.kernel.pipeline;
