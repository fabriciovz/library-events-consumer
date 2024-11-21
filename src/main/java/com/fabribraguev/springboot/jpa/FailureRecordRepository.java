package com.fabribraguev.springboot.jpa;

import com.fabribraguev.springboot.entity.FailedRecord;
import org.springframework.data.repository.CrudRepository;

public interface FailureRecordRepository extends CrudRepository<FailedRecord,Integer> {
}
