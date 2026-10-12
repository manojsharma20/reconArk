package io.reconark.worker;

import io.reconark.engine.messages.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the same topics as the manager. Whichever starts first creates them with the right number of partitions;
 * declaring an existing topic does nothing. (If a consumer could create a topic implicitly, it would get only one
 * partition and only one worker would ever receive jobs.)
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
