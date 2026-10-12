package io.reconark.manager.messaging;

import io.reconark.engine.messages.JobResultMessage;
import io.reconark.engine.messages.Topics;
import io.reconark.manager.run.RunService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Receives job results from the workers and hands them to {@link RunService}. */
@Component
public class JobResultListener {

    private final RunService runs;
    private final JsonMapper json;

    public JobResultListener(RunService runs, JsonMapper json) {
        this.runs = runs;
        this.json = json;
    }

    @KafkaListener(topics = Topics.JOB_RESULTS, groupId = "recon-manager")
    public void onMessage(String message) {
        runs.onJobResult(json.readValue(message, JobResultMessage.class));
    }
}
