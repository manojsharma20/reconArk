package io.reconark.manager.messaging;

import io.reconark.engine.messages.JobMessage;
import io.reconark.engine.messages.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends jobs to the workers. The run id is the Kafka key, so all messages of one run go to the same partition.
 *
 * <p>If sending fails, nothing is lost: the job stays QUEUED in the database and the watchdog in
 * {@link io.reconark.manager.run.RunService} sends it again after the job timeout.
 */
@Component
public class JobPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public JobPublisher(KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    public void publish(JobMessage job) {
        try {
            kafka.send(Topics.JOBS, job.runId().toString(), json.writeValueAsString(job))
                    .whenComplete((ok, error) -> {
                        if (error != null) {
                            warn(job, error);
                        }
                    });
        } catch (RuntimeException error) { // e.g. Kafka unreachable: send() gives up after max.block.ms
            warn(job, error);
        }
    }

    private static void warn(JobMessage job, Throwable error) {
        log.warn("Could not send job {} of run {}; the watchdog will send it again: {}",
                job.jobId(), job.runId(), error.toString());
    }
}
