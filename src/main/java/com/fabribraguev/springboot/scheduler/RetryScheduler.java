package com.fabribraguev.springboot.scheduler;


import com.fabribraguev.springboot.config.LibraryEventsConsumerConfig;
import com.fabribraguev.springboot.entity.FailureRecord;
import com.fabribraguev.springboot.jpa.FailureRecordRepository;
import com.fabribraguev.springboot.service.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryScheduler {

    @Autowired
    private FailureRecordRepository failureRecordRepository;

    @Autowired
    private LibraryEventsService libraryEventsService;

    @Scheduled(cron = "*/10 * * * * *") //every ten seconds
    public void retryFailedRecords(){
        log.info("Retrying Failed Records Started!");
        failureRecordRepository.findAllByStatus(LibraryEventsConsumerConfig.RETRY)
                .forEach(failureRecord -> {
                    log.info("Retrying Failed Records: {} ", failureRecord);
                    var consumerRecord = buildConsumerRecord(failureRecord);
                    try {
                        libraryEventsService.processLibraryEvent(consumerRecord);
                        failureRecord.setStatus(LibraryEventsConsumerConfig.SUCCESS);
                    } catch (JsonProcessingException e) {
                       log.error("Exception in retryFailedRecords: {} ", e.getMessage(), e);
                    }
                });
        log.info("Retrying Failed Records Completed!");
    }

    private ConsumerRecord<Integer,String> buildConsumerRecord(FailureRecord failureRecord) {
        return new ConsumerRecord<>(
                failureRecord.getTopic(),
                failureRecord.getPartition(),
                failureRecord.getOffsetValue(),
                failureRecord.getKeyValue(),
                failureRecord.getErrorRecord()
        );
    }
}
