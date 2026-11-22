package com.example.library.services;

import com.example.library.dtos.CategoryRequestDTO;
import com.example.library.dtos.CategoryResponseDTO;
import com.example.library.entities.Category;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.repositories.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> findAll() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(category -> new CategoryResponseDTO(category)).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(Long id) {
        Category category = searchCategory(id);
        return new CategoryResponseDTO(category);
    }

    @Transactional
    public CategoryResponseDTO insert(CategoryRequestDTO request) {
        Category category  = new Category(null, request.name());
        categoryRepository.save(category);
        return new CategoryResponseDTO(category);
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO request) {
        Category category = searchCategory(id);
        category.setName(request.name());
        return new CategoryResponseDTO(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = searchCategory(id);
        categoryRepository.delete(category);
    }

    private Category searchCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
