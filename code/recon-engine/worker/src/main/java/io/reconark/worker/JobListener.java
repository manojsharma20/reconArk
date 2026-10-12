package io.reconark.worker;

import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.JobResultMessage;
import io.reconark.engine.messages.Topics;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Takes jobs from Kafka, runs them and sends the result back.
 *
 * <p>All workers use the consumer group {@code recon-workers}, so Kafka gives each job to exactly one worker. The
 * message is acknowledged only after the result has been sent: if a worker dies half-way, Kafka gives the job to
 * another worker. Jobs are written so that running them twice gives the same outcome.
 */
@Component
public class JobListener {

    private final JobHandler handler;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public JobListener(JobHandler handler, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.handler = handler;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = Topics.JOBS, groupId = "recon-workers", concurrency = "${reconark.worker.concurrency:2}")
    public void onMessage(String message) throws InterruptedException, ExecutionException, TimeoutException {
        JobMessage job = json.readValue(message, JobMessage.class);
        JobResultMessage result = handler.handle(job);
        kafka.send(Topics.JOB_RESULTS, job.runId().toString(), json.writeValueAsString(result)).get(30, TimeUnit.SECONDS);
    }
}
