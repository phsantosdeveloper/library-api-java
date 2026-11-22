package com.example.library.services;

import com.example.library.dtos.CategoryRequestDTO;
import com.example.library.dtos.CategoryResponseDTO;
import com.example.library.entities.Category;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldReturnListOfCategory() {

        //arrange
        Category category = new Category(1L, "Action");
        Category category1 = new Category(2L, "Ficção");

        when(categoryRepository.findAll()).thenReturn(List.of(category, category1));

        //act
        List<CategoryResponseDTO> result = categoryService.findAll();

        //assert
        assertEquals(2, result.size());
        assertEquals(new CategoryResponseDTO(category), result.get(0));
        assertEquals(new CategoryResponseDTO(category1), result.get(1));
    }

    @Test
    void shouldReturnEmptyCategoryList() {

        //arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        //act
        List<CategoryResponseDTO> result = categoryService.findAll();

        //assert
        assertEquals(0, result.size());
    }

    @Test
    void shouldReturnCategory() {

        //arrange
        Category category = new Category(1L, "Action");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        //act
        CategoryResponseDTO result = categoryService.findById(1L);

        //assert
        assertEquals(new CategoryResponseDTO(category), result);
        verify(categoryRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNonExistentCategory() {

        //arrange
       when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.findById(99L)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        verify(categoryRepository).findById(99L);
    }

    @Test
    void shouldInsertCategoryWithValidData() {

        //arrage
        CategoryRequestDTO request = new CategoryRequestDTO("Action");

        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> {
            Category category = call.getArgument(0);
            category.setId(1L);
            return category;
        });

        //act
        CategoryResponseDTO result = categoryService.insert(request);
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category categoryCaptured = captor.getValue();

        //assert
        assertEquals(new CategoryResponseDTO(1L, "Action"), result);
        assertEquals(1L, categoryCaptured.getId());
        assertEquals("Action", categoryCaptured.getName());
    }

    @Test
    void shouldUpdateCategoryWithValidData() {

        //arrage
        Category category = new Category(1L, "Fiction");
        CategoryRequestDTO request = new CategoryRequestDTO("Science fiction");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        //act
        CategoryResponseDTO result = categoryService.update(1L, request);

        //assert
        assertEquals(new CategoryResponseDTO(1L, "Science fiction"), result);
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowExceptionWhenUpdateForNonExistentCategory() {

        //arrage
        CategoryRequestDTO request = new CategoryRequestDTO("Action");

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.update(99L, request)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldDeleteCategory() {

        //arrage
        Category category = new Category(1L, "Action");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        //act
        categoryService.delete(1L);

        //assert
        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldReturnExceptionOnDeleteNonExistingCategory() {

        //arrage
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.delete(99L)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
