package com.fabribraguev.springboot.consumer;

import com.fabribraguev.springboot.entity.Book;
import com.fabribraguev.springboot.entity.LibraryEvent;
import com.fabribraguev.springboot.entity.LibraryEventType;
import com.fabribraguev.springboot.jpa.FailureRecordRepository;
import com.fabribraguev.springboot.jpa.LibraryEventsRepository;
import com.fabribraguev.springboot.service.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 1)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventsConsumerIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    LibraryEventsRepository libraryEventsRepository;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    LibraryEventsService libraryEventsServiceSpy;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FailureRecordRepository failureRecordRepository;

    private Consumer<Integer, String> consumer;


    @BeforeEach
    void setUp(){
        var container = endpointRegistry.getListenerContainers().stream().filter(messageListenerContainer -> messageListenerContainer.getGroupId().
                equals("library-events-listener-group-1")).
                collect(Collectors.toList()).get(0);
        ContainerTestUtils.waitForAssignment(container,embeddedKafkaBroker.getPartitionsPerTopic());
        /*for(MessageListenerContainer messageListenerContainer: endpointRegistry.getListenerContainers()){
            ContainerTestUtils.waitForAssignment(messageListenerContainer,embeddedKafkaBroker.getPartitionsPerTopic());
        }*/
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
        failureRecordRepository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = "{\n" +
                    "    \"libraryEventId\": null,\n" +
                    "    \"libraryEventType\": \"NEW\",\n" +
                    "    \"book\": {\n" +
                    "        \"bookId\": 456,\n" +
                    "        \"bookName\": \"Kafka Using Spring Boot\",\n" +
                    "        \"bookAuthor\": \"Dilip\"\n" +
                    "    }\n" +
                    "}";
        kafkaTemplate.sendDefault(json).get();
        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy,times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy,times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEventList = (List<LibraryEvent>) libraryEventsRepository.findAll();

        assertEquals(1,libraryEventList.size());

        libraryEventList.forEach(libraryEvent -> {
            assertNotNull(libraryEvent.getLibraryEventId());
            assertEquals(456,libraryEvent.getBook().getBookId());
        });
    }

    @Test
    void publishUpdateLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {
        //given
        //Save the new library event
        String json = "{\n" +
                "    \"libraryEventId\": null,\n" +
                "    \"libraryEventType\": \"NEW\",\n" +
                "    \"book\": {\n" +
                "        \"bookId\": 456,\n" +
                "        \"bookName\": \"Kafka Using Spring Boot\",\n" +
                "        \"bookAuthor\": \"Dilip\"\n" +
                "    }\n" +
                "}";
        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);

        //Publish the update library event
       Book updatedBook= Book.builder().bookId(456).bookName("Kafka Using Spring Boot 2.0X").bookAuthor("Fabricio").build();
       libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
       libraryEvent.setBook(updatedBook);
        String updatedJson = objectMapper.writeValueAsString(libraryEvent);
        kafkaTemplate.sendDefault(libraryEvent.getLibraryEventId(),updatedJson).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy,times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy,times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        LibraryEvent persistedLibraryEvent =  libraryEventsRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("Kafka Using Spring Boot 2.0X",persistedLibraryEvent.getBook().getBookName());
    }

    @Test
    void publishUpdateLibraryEvent_null_LibraryEvent_failureRecord() throws JsonProcessingException, ExecutionException, InterruptedException {
        //given
        //Save the new library event
        String json = "{\n" +
                "    \"libraryEventId\": null,\n" +
                "    \"libraryEventType\": \"UPDATE\",\n" +
                "    \"book\": {\n" +
                "        \"bookId\": 456,\n" +
                "        \"bookName\": \"Kafka Using Spring Boot\",\n" +
                "        \"bookAuthor\": \"Dilip\"\n" +
                "    }\n" +
                "}";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy,times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy,times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        var count= failureRecordRepository.count();

        assertEquals(1, count);
        assertEquals(1, failureRecordRepository.findAllByStatus("DEAD").size());

        failureRecordRepository.findAll()
                .forEach(failedRecord -> {
                    System.out.println("Failure record : " + failedRecord);
                });
    }

    @Test
    void publishUpdateLibraryEvent_999_LibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {
        //given
        //Save the new library event
        String json = "{\n" +
                "    \"libraryEventId\": 999,\n" +
                "    \"libraryEventType\": \"UPDATE\",\n" +
                "    \"book\": {\n" +
                "        \"bookId\": 456,\n" +
                "        \"bookName\": \"Kafka Using Spring Boot\",\n" +
                "        \"bookAuthor\": \"Dilip\"\n" +
                "    }\n" +
                "}";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy,times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy,times(3)).processLibraryEvent(isA(ConsumerRecord.class));

        var count= failureRecordRepository.count();
        assertEquals(1, count);
        assertEquals(1, failureRecordRepository.findAllByStatus("RETRY").size());

        failureRecordRepository.findAll()
                .forEach(failedRecord -> {
                    System.out.println("Failure record : " + failedRecord);
                });
    }
}
