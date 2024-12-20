package com.fabribraguev.springboot.jpa;

import com.fabribraguev.springboot.entity.FailureRecord;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FailureRecordRepository extends CrudRepository<FailureRecord,Integer> {
    List<FailureRecord> findAllByStatus(String status);
}
