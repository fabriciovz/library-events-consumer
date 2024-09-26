package com.fabribraguev.springboot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LibraryEvent {
    Integer libraryEventId;
    LibraryEventType libraryEventType;
    @NotNull
    @Valid
    Book book;
}
