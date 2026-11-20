package com.example.library.controllers;

import com.example.library.dtos.AuthorRequestDTO;
import com.example.library.dtos.AuthorResponseDTO;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.services.AuthorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
@ActiveProfiles("test")
public class AuthorControllerTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    // ---------- GET /authors ----------

    @Test
    void shouldReturn200WhenPaginatingAllRegisteredAuthors() throws Exception {

        //arrange
        List<AuthorResponseDTO> dtos = List.of(
                new AuthorResponseDTO(1L, "Machado de Assis"),
                new AuthorResponseDTO(2L, "Jorge Amado")
        );
        when(authorService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(dtos, PageRequest.of(0, 20), 2));

        //act + assert
        mockMvc.perform(get("/authors")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Machado de Assis"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Jorge Amado"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(authorService).findAll(captor.capture());
        Pageable received = captor.getValue();
        assertThat(received.getPageNumber()).isEqualTo(0);
        assertThat(received.getPageSize()).isEqualTo(20);
        assertThat(received.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldReturn200WhenPaginationIsEmpty() throws Exception {

        //arrange
        when(authorService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        //act + assert
        mockMvc.perform(get("/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(authorService).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    // ---------- GET /authors/{id} ----------

    @Test
    void shouldReturn200WhenFindingExistingAuthor() throws Exception {

        //arrange
        when(authorService.findById(1L)).thenReturn(new AuthorResponseDTO(1L, "Machado de Assis"));

        //act + assert
        mockMvc.perform(get("/authors/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Machado de Assis"));

        verify(authorService).findById(1L);
    }

    @Test
    void shouldReturn404WhenFindingNonExistentAuthor() throws Exception {

        //arrange
        when(authorService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act + assert
        mockMvc.perform(get("/authors/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/authors/99"));

        verify(authorService).findById(99L);
    }

    // ---------- POST /authors ----------

    @Test
    void shouldReturn201WhenInsertingAuthorWithValidData() throws Exception {

        //arrange
        when(authorService.insert(any(AuthorRequestDTO.class)))
                .thenReturn(new AuthorResponseDTO(10L, "Clarice Lispector"));

        String json = """
                {
                    "name": "Clarice Lispector"
                }
                """;

        //act + assert
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/authors/10"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Clarice Lispector"));

        ArgumentCaptor<AuthorRequestDTO> captor = ArgumentCaptor.forClass(AuthorRequestDTO.class);
        verify(authorService).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new AuthorRequestDTO("Clarice Lispector"));
    }

    @Test
    void shouldReturn422WhenInsertingAuthorWithEmptyName() throws Exception {

        //arrange
        String json = """
                {
                    "name": ""
                }
                """;

        //act + assert
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).insert(any(AuthorRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingAuthorWithBlankName() throws Exception {

        //arrange
        String json = """
                {
                    "name": " "
                }
                """;

        //act + assert
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).insert(any(AuthorRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingAuthorWithNullName() throws Exception {

        //arrange
        String json = """
                {
                    "name": null
                }
                """;

        //act + assert
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).insert(any(AuthorRequestDTO.class));
    }

    // ---------- PUT /authors/{id} ----------

    @Test
    void shouldReturn200WhenUpdatingAuthorWithValidData() throws Exception {

        //arrange
        when(authorService.update(eq(1L), any(AuthorRequestDTO.class)))
                .thenReturn(new AuthorResponseDTO(1L, "Machado de Assis"));

        String json = """
                {
                    "name": "Machado de Assis"
                }
                """;

        //act + assert
        mockMvc.perform(put("/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Machado de Assis"));

        ArgumentCaptor<AuthorRequestDTO> captor = ArgumentCaptor.forClass(AuthorRequestDTO.class);
        verify(authorService).update(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new AuthorRequestDTO("Machado de Assis"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAuthor() throws Exception {

        //arrange
        when(authorService.update(eq(99L), any(AuthorRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        String json = """
                {
                    "name": "Machado de Assis"
                }
                """;

        //act + assert
        mockMvc.perform(put("/authors/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/authors/99"));

        verify(authorService).update(eq(99L), any(AuthorRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingAuthorWithEmptyName() throws Exception {

        //arrange
        String json = """
                {
                    "name": ""
                }
                """;

        //act + assert
        mockMvc.perform(put("/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).update(anyLong(), any(AuthorRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingAuthorWithBlankName() throws Exception {

        //arrange
        String json = """
                {
                    "name": " "
                }
                """;

        //act + assert
        mockMvc.perform(put("/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).update(anyLong(), any(AuthorRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingAuthorWithNullName() throws Exception {

        //arrange
        String json = """
                {
                    "name": null
                }
                """;

        //act + assert
        mockMvc.perform(put("/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required"));

        verify(authorService, never()).update(anyLong(), any(AuthorRequestDTO.class));
    }

    // ---------- DELETE /authors/{id} ----------

    @Test
    void shouldReturn204WhenDeletingExistingAuthor() throws Exception {

        //act + assert
        mockMvc.perform(delete("/authors/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(authorService).delete(1L);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAuthor() throws Exception {

        //arrange
        doThrow(new ResourceNotFoundException(99L)).when(authorService).delete(99L);

        //act + assert
        mockMvc.perform(delete("/authors/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/authors/99"));

        verify(authorService).delete(99L);
    }
}
