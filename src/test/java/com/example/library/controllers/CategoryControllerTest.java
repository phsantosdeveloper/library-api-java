package com.example.library.controllers;

import com.example.library.config.ResourceServerConfig;
import com.example.library.dtos.CategoryRequestDTO;
import com.example.library.dtos.CategoryResponseDTO;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.services.CategoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
@Import(ResourceServerConfig.class)
@EnableConfigurationProperties(H2ConsoleProperties.class)
public class CategoryControllerTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // ---------- GET /categories ----------

    @Test
    void shouldReturn200WhenListingAllCategories() throws Exception {

        //arrange
        when(categoryService.findAll()).thenReturn(List.of(
                new CategoryResponseDTO(1L, "Action"),
                new CategoryResponseDTO(2L, "Fiction")
        ));

        //act and assert
        mockMvc.perform(get("/categories").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Fiction"));

        verify(categoryService).findAll();
    }

    @Test
    void shouldReturn200WhenCategoryListIsEmpty() throws Exception {

        //arrange
        when(categoryService.findAll()).thenReturn(List.of());

        //act and assert
        mockMvc.perform(get("/categories").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(categoryService).findAll();
    }

    // ---------- GET /categories/{id} ----------

    @Test
    void shouldReturn200WhenFindingExistingCategory() throws Exception {

        //arrange
        when(categoryService.findById(1L)).thenReturn(new CategoryResponseDTO(1L, "Action"));

        //act and assert
        mockMvc.perform(get("/categories/{id}", 1L).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Action"));

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldReturn404WhenFindingNonExistentCategory() throws Exception {

        //arrange
        when(categoryService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(get("/categories/{id}", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/categories/99"));

        verify(categoryService).findById(99L);
    }

    // ---------- POST /categories ----------

    @Test
    void shouldReturn201WhenInsertingCategoryWithValidData() throws Exception {

        //arrange
        when(categoryService.insert(any(CategoryRequestDTO.class)))
                .thenReturn(new CategoryResponseDTO(10L, "Romance"));

        String json = """
                {
                    "name": "Romance"
                }
                """;

        //act and assert
        mockMvc.perform(post("/categories").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/categories/10"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Romance"));

        ArgumentCaptor<CategoryRequestDTO> captor = ArgumentCaptor.forClass(CategoryRequestDTO.class);
        verify(categoryService).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new CategoryRequestDTO("Romance"));
    }

    @Test
    void shouldReturn422WhenInsertingCategoryWithEmptyName() throws Exception {

        //arrange
        String json = """
                {
                    "name": ""
                }
                """;

        //act and assert
        mockMvc.perform(post("/categories").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).insert(any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingCategoryWithBlankName() throws Exception {

        //arrange
        String json = """
                {
                    "name": " "
                }
                """;

        //act and assert
        mockMvc.perform(post("/categories").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).insert(any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingCategoryWithNullName() throws Exception {

        //arrange
        String json = """
                {
                    "name": null
                }
                """;

        //act and assert
        mockMvc.perform(post("/categories").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).insert(any(CategoryRequestDTO.class));
    }

    // ---------- PUT /categories/{id} ----------

    @Test
    void shouldReturn200WhenUpdatingCategoryWithValidData() throws Exception {

        //arrange
        when(categoryService.update(eq(1L), any(CategoryRequestDTO.class)))
                .thenReturn(new CategoryResponseDTO(1L, "Science fiction"));

        String json = """
                {
                    "name": "Science fiction"
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Science fiction"));

        ArgumentCaptor<CategoryRequestDTO> captor = ArgumentCaptor.forClass(CategoryRequestDTO.class);
        verify(categoryService).update(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new CategoryRequestDTO("Science fiction"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCategory() throws Exception {

        //arrange
        when(categoryService.update(eq(99L), any(CategoryRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        String json = """
                {
                    "name": "Science fiction"
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 99L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/categories/99"));

        verify(categoryService).update(eq(99L), any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingCategoryWithEmptyName() throws Exception {

        //arrange
        String json = """
                {
                    "name": ""
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).update(anyLong(), any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingCategoryWithBlankName() throws Exception {

        //arrange
        String json = """
                {
                    "name": " "
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).update(anyLong(), any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingCategoryWithNullName() throws Exception {

        //arrange
        String json = """
                {
                    "name": null
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(categoryService, never()).update(anyLong(), any(CategoryRequestDTO.class));
    }

    // ---------- DELETE /categories/{id} ----------

    @Test
    void shouldReturn204WhenDeletingExistingCategory() throws Exception {

        //act and assert
        mockMvc.perform(delete("/categories/{id}", 1L).with(admin()))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCategory() throws Exception {

        //arrange
        doThrow(new ResourceNotFoundException(99L)).when(categoryService).delete(99L);

        //act and assert
        mockMvc.perform(delete("/categories/{id}", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/categories/99"));

        verify(categoryService).delete(99L);
    }

    // ---------- SECURITY ----------

    @Test
    void shouldReturn401WhenRequestHasNoToken() throws Exception {

        //act and assert
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldReturn200WhenUserFindsCategoryById() throws Exception {

        //arrange
        when(categoryService.findById(1L)).thenReturn(new CategoryResponseDTO(1L, "Action"));

        //act and assert
        mockMvc.perform(get("/categories/{id}", 1L).with(user()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserTriesToInsertCategory() throws Exception {

        //arrange
        String json = """
                {
                    "name": "Romance"
                }
                """;

        //act and assert
        mockMvc.perform(post("/categories").with(user()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).insert(any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn403WhenUserTriesToUpdateCategory() throws Exception {

        //arrange
        String json = """
                {
                    "name": "Science fiction"
                }
                """;

        //act and assert
        mockMvc.perform(put("/categories/{id}", 1L).with(user()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).update(anyLong(), any(CategoryRequestDTO.class));
    }

    @Test
    void shouldReturn403WhenUserTriesToDeleteCategory() throws Exception {

        //act and assert
        mockMvc.perform(delete("/categories/{id}", 1L).with(user()))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).delete(anyLong());
    }
}
