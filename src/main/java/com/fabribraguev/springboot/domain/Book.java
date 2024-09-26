package com.fabribraguev.springboot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Book {
    @NotNull
    Integer bookId;
    @NotBlank
    String bookName;
    @NotBlank
    String bookAuthor;
}
