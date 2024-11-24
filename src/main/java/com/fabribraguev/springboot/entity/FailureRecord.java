package com.fabribraguev.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FailureRecord {
    @Id
    @GeneratedValue
    private Integer id;
    private String topic;
    private Integer keyValue;
    private String errorRecord;
    private Integer partition;
    private Long offsetValue;
    private String excelption;
    private String status;


}
