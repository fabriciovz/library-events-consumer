package com.fabribraguev.springboot.jpa;

import com.fabribraguev.springboot.entity.LibraryEvent;
import org.springframework.data.repository.CrudRepository;

public interface LibraryEventsRepository extends CrudRepository<LibraryEvent, Integer> {
}
