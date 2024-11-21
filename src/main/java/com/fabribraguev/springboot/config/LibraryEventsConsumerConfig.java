package com.fabribraguev.springboot.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {
    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;

    public DeadLetterPublishingRecoverer publishingRecoverer(){

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (r, e) -> {
                    log.error("Exception in publishingRecoverer: {} ",e.getMessage(),e);
                    if (e.getCause() instanceof RecoverableDataAccessException) {
                        return new TopicPartition(retryTopic, r.partition());
                    }
                    else {
                        return new TopicPartition(deadLetterTopic, r.partition());
                    }
                });
        return recoverer;
    }

    public DefaultErrorHandler errorHandler() {


        var exceptionToIgnoreList = List.of(
                IllegalArgumentException.class
        );
        /*var exceptionToRetryList = List.of(
                RecoverableDataAccessException.class
        );*/

        //var fixedBackOff = new FixedBackOff(1000L, 2L);  //Retry the record twice once second (1000ms) in between (2 + 1 (the original attempt)

        var expBackOff = new ExponentialBackOffWithMaxRetries(2);
        expBackOff.setInitialInterval(1_000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2_000L);

        /*2024-11-17 14:55:33.452
        2024-11-17 14:55:34.504
        2024-11-17 14:55:36.581   */

        var defaultErrorHandler = new DefaultErrorHandler(
                publishingRecoverer(),
                //fixedBackOff
                expBackOff
        );
        exceptionToIgnoreList.forEach(defaultErrorHandler::addNotRetryableExceptions);
        //exceptionToRetryList.forEach(defaultErrorHandler::addRetryableExceptions);

        //You can hide this in prod environment
        defaultErrorHandler.setRetryListeners(((consumerRecord, exception, deliveryAttempt) -> {
            log.info("Failed Record in Retry Listener, Exception : {}, deliveryAttempt: {}", exception.getMessage(), deliveryAttempt);
        } ));

        return defaultErrorHandler;
    }
    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer, ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setConcurrency(1);
        factory.setCommonErrorHandler(errorHandler()); //attempts
        log.info("Kafka listerner manual config here");
        return factory;
    }

}
