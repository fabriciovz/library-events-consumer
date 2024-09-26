package com.fabribraguev.springboot.entity;

import com.fabribraguev.springboot.entity.Book;
import com.fabribraguev.springboot.entity.LibraryEventType;
import lombok.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LibraryEvent {
    @Id
    @GeneratedValue
    Integer libraryEventId;
    @Enumerated(EnumType.STRING)
    LibraryEventType libraryEventType;
    @OneToOne(mappedBy = "libraryEvent",cascade = {CascadeType.ALL})
    @ToString.Exclude
    Book book;
}
