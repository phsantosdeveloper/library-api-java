package com.example.library.mappers;

import com.example.library.dtos.BookRequestDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.entities.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "loans", ignore = true)
    Book toEntity(BookRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalCopies", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "loans", ignore = true)
    void updateEntity(BookUpdateDTO request, @MappingTarget Book book);
}
