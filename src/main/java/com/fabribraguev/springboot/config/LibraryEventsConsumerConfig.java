package com.fabribraguev.springboot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {

    public DefaultErrorHandler errorHandler() {

        /*
        var exceptionToIgnoreList = List.of(
                IllegalArgumentException.class
        );*/
        var exceptionToRetryList = List.of(
                RecoverableDataAccessException.class
        );

        var fixedBackOff = new FixedBackOff(1000L, 2L);  //Retry the record twice once second (1000ms) in between (2 + 1 (the original attempt)
        var defaultErrorHandler = new DefaultErrorHandler(
                fixedBackOff
        );
        //exceptionToIgnoreList.forEach(defaultErrorHandler::addNotRetryableExceptions);
        exceptionToRetryList.forEach(defaultErrorHandler::addRetryableExceptions);

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
