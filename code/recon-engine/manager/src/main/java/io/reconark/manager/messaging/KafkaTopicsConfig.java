package io.reconark.manager.messaging;

import io.reconark.engine.messages.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates the Kafka topics at start-up if they don't exist (workers declare the same topics). The number of partitions
 * of the jobs topic is the maximum number of jobs that can run at the same time, so keep it at least as high as
 * workers x concurrency.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaTopicsConfig {

    @Bean
    NewTopic jobsTopic(@Value("${reconark.kafka.partitions:12}") int partitions,
            @Value("${reconark.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.JOBS).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic jobResultsTopic(@Value("${reconark.kafka.partitions:12}") int partitions,
            @Value("${reconark.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.JOB_RESULTS).partitions(partitions).replicas(replicas).build();
    }
}
