package com.example.library.controllers;

import com.example.library.config.ResourceServerConfig;
import com.example.library.dtos.AuthorResponseDTO;
import com.example.library.dtos.BookCopiesDTO;
import com.example.library.dtos.BookRequestDTO;
import com.example.library.dtos.BookResponseDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.dtos.CategoryResponseDTO;
import com.example.library.exceptions.InvalidWithdrawalAmountException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.services.BookService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@ActiveProfiles("test")
@Import(ResourceServerConfig.class)
@EnableConfigurationProperties(H2ConsoleProperties.class)
public class BookControllerTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";

    private static final String VALID_REQUEST_JSON = """
            {
                "title": "Dom Casmurro",
                "isbn": "9788535914849",
                "publicationDate": "1899-01-01",
                "totalCopies": 10,
                "authorId": 1,
                "categoryId": 1
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
                "title": "Dom Casmurro - Edição Revisada",
                "isbn": "9788535914849",
                "publicationDate": "1900-01-01",
                "authorId": 1,
                "categoryId": 1
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static BookResponseDTO bookResponse(Long id, String title, Integer totalCopies) {
        return new BookResponseDTO(id, title, "9788535914849", LocalDate.of(1899, 1, 1), totalCopies,
                new AuthorResponseDTO(1L, "Machado de Assis"), new CategoryResponseDTO(1L, "Romance"));
    }

    // ---------- GET /books ----------

    @Test
    void shouldReturn200WhenPaginatingAllRegisteredBooks() throws Exception {

        //arrange
        List<BookResponseDTO> dtos = List.of(
                bookResponse(1L, "Dom Casmurro", 10),
                new BookResponseDTO(2L, "Effective Java", "9780134685991", LocalDate.of(2018, 1, 6), 5,
                        new AuthorResponseDTO(2L, "Joshua Bloch"), new CategoryResponseDTO(2L, "Technology"))
        );
        when(bookService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(dtos, PageRequest.of(0, 20), 2));

        //act and assert
        mockMvc.perform(get("/books").with(admin())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.content[0].isbn").value("9788535914849"))
                .andExpect(jsonPath("$.content[0].publicationDate").value("1899-01-01"))
                .andExpect(jsonPath("$.content[0].totalCopies").value(10))
                .andExpect(jsonPath("$.content[0].author.id").value(1))
                .andExpect(jsonPath("$.content[0].author.name").value("Machado de Assis"))
                .andExpect(jsonPath("$.content[0].category.id").value(1))
                .andExpect(jsonPath("$.content[0].category.name").value("Romance"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].title").value("Effective Java"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).findAll(isNull(), isNull(), isNull(), isNull(), captor.capture());
        Pageable received = captor.getValue();
        assertThat(received.getPageNumber()).isEqualTo(0);
        assertThat(received.getPageSize()).isEqualTo(20);
        assertThat(received.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldReturn200WhenPaginatingBooksWithFilters() throws Exception {

        //arrange
        when(bookService.findAll(eq("casmurro"), eq("9788535914849"), eq("machado"), eq("romance"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookResponse(1L, "Dom Casmurro", 10)), PageRequest.of(0, 20), 1));

        //act and assert
        mockMvc.perform(get("/books").with(admin())
                        .param("title", "casmurro")
                        .param("isbn", "9788535914849")
                        .param("author", "machado")
                        .param("category", "romance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookService).findAll(eq("casmurro"), eq("9788535914849"), eq("machado"), eq("romance"),
                any(Pageable.class));
    }

    @Test
    void shouldReturn200WhenPaginationIsEmpty() throws Exception {

        //arrange
        when(bookService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        //act and assert
        mockMvc.perform(get("/books").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).findAll(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    // ---------- GET /books/{id} ----------

    @Test
    void shouldReturn200WhenFindingExistingBook() throws Exception {

        //arrange
        when(bookService.findById(1L)).thenReturn(bookResponse(1L, "Dom Casmurro", 10));

        //act and assert
        mockMvc.perform(get("/books/{id}", 1L).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.isbn").value("9788535914849"))
                .andExpect(jsonPath("$.publicationDate").value("1899-01-01"))
                .andExpect(jsonPath("$.totalCopies").value(10))
                .andExpect(jsonPath("$.author.id").value(1))
                .andExpect(jsonPath("$.author.name").value("Machado de Assis"))
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.name").value("Romance"));

        verify(bookService).findById(1L);
    }

    @Test
    void shouldReturn404WhenFindingNonExistentBook() throws Exception {

        //arrange
        when(bookService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(get("/books/{id}", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books/99"));

        verify(bookService).findById(99L);
    }

    // ---------- POST /books ----------

    @Test
    void shouldReturn201WhenInsertingBookWithValidData() throws Exception {

        //arrange
        when(bookService.insert(any(BookRequestDTO.class)))
                .thenReturn(bookResponse(10L, "Dom Casmurro", 10));

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/books/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.isbn").value("9788535914849"))
                .andExpect(jsonPath("$.publicationDate").value("1899-01-01"))
                .andExpect(jsonPath("$.totalCopies").value(10))
                .andExpect(jsonPath("$.author.id").value(1))
                .andExpect(jsonPath("$.category.id").value(1));

        ArgumentCaptor<BookRequestDTO> captor = ArgumentCaptor.forClass(BookRequestDTO.class);
        verify(bookService).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookRequestDTO("Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 10, 1L, 1L));
    }

    @Test
    void shouldReturn404WhenInsertingBookWithNonExistentAuthor() throws Exception {

        //arrange
        when(bookService.insert(any(BookRequestDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books"));

        verify(bookService).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingBookWithNullFields() throws Exception {

        //arrange
        String json = """
                {
                    "title": null,
                    "isbn": null,
                    "publicationDate": null,
                    "totalCopies": null,
                    "authorId": null,
                    "categoryId": null
                }
                """;

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(6))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'title')].message").value("Title is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'isbn')].message").value("ISBN is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'publicationDate')].message").value("Publication date is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'totalCopies')].message").value("Total copies is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'authorId')].message").value("Author is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'categoryId')].message").value("Category is required"));

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingBookWithBlankTitle() throws Exception {

        //arrange
        String json = """
                {
                    "title": " ",
                    "isbn": "9788535914849",
                    "publicationDate": "1899-01-01",
                    "totalCopies": 10,
                    "authorId": 1,
                    "categoryId": 1
                }
                """;

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("title"))
                .andExpect(jsonPath("$.errors[0].message").value("Title is required"));

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingBookWithInvalidIsbn() throws Exception {

        //arrange
        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "1234567890123",
                    "publicationDate": "1899-01-01",
                    "totalCopies": 10,
                    "authorId": 1,
                    "categoryId": 1
                }
                """;

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("isbn"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid ISBN"));

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingBookWithFuturePublicationDate() throws Exception {

        //arrange
        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "9788535914849",
                    "publicationDate": "%s",
                    "totalCopies": 10,
                    "authorId": 1,
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(1));

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("publicationDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid date"));

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn422WhenInsertingBookWithNegativeTotalCopies() throws Exception {

        //arrange
        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "9788535914849",
                    "publicationDate": "1899-01-01",
                    "totalCopies": -1,
                    "authorId": 1,
                    "categoryId": 1
                }
                """;

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("totalCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid number"));

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn201WhenInsertingBookWithZeroTotalCopies() throws Exception {

        //arrange
        when(bookService.insert(any(BookRequestDTO.class)))
                .thenReturn(bookResponse(10L, "Dom Casmurro", 0));

        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "9788535914849",
                    "publicationDate": "1899-01-01",
                    "totalCopies": 0,
                    "authorId": 1,
                    "categoryId": 1
                }
                """;

        //act and assert
        mockMvc.perform(post("/books").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCopies").value(0));

        verify(bookService).insert(any(BookRequestDTO.class));
    }

    // ---------- PUT /books/{id} ----------

    @Test
    void shouldReturn200WhenUpdatingBookWithValidData() throws Exception {

        //arrange
        when(bookService.update(eq(1L), any(BookUpdateDTO.class)))
                .thenReturn(new BookResponseDTO(1L, "Dom Casmurro - Edição Revisada", "9788535914849",
                        LocalDate.of(1900, 1, 1), 10,
                        new AuthorResponseDTO(1L, "Machado de Assis"), new CategoryResponseDTO(1L, "Romance")));

        //act and assert
        mockMvc.perform(put("/books/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Dom Casmurro - Edição Revisada"))
                .andExpect(jsonPath("$.publicationDate").value("1900-01-01"))
                .andExpect(jsonPath("$.totalCopies").value(10));

        ArgumentCaptor<BookUpdateDTO> captor = ArgumentCaptor.forClass(BookUpdateDTO.class);
        verify(bookService).update(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookUpdateDTO("Dom Casmurro - Edição Revisada",
                "9788535914849", LocalDate.of(1900, 1, 1), 1L, 1L));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentBook() throws Exception {

        //arrange
        when(bookService.update(eq(99L), any(BookUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(put("/books/{id}", 99L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books/99"));

        verify(bookService).update(eq(99L), any(BookUpdateDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingBookWithNullFields() throws Exception {

        //arrange
        String json = """
                {
                    "title": null,
                    "isbn": null,
                    "publicationDate": null,
                    "authorId": null,
                    "categoryId": null
                }
                """;

        //act and assert
        mockMvc.perform(put("/books/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(5))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'title')].message").value("Title is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'isbn')].message").value("ISBN is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'publicationDate')].message").value("Publication date is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'authorId')].message").value("Author is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'categoryId')].message").value("Category is required"));

        verify(bookService, never()).update(anyLong(), any(BookUpdateDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingBookWithInvalidIsbn() throws Exception {

        //arrange
        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "1234567890123",
                    "publicationDate": "1899-01-01",
                    "authorId": 1,
                    "categoryId": 1
                }
                """;

        //act and assert
        mockMvc.perform(put("/books/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("isbn"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid ISBN"));

        verify(bookService, never()).update(anyLong(), any(BookUpdateDTO.class));
    }

    @Test
    void shouldReturn422WhenUpdatingBookWithFuturePublicationDate() throws Exception {

        //arrange
        String json = """
                {
                    "title": "Dom Casmurro",
                    "isbn": "9788535914849",
                    "publicationDate": "%s",
                    "authorId": 1,
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(1));

        //act and assert
        mockMvc.perform(put("/books/{id}", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("publicationDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid date"));

        verify(bookService, never()).update(anyLong(), any(BookUpdateDTO.class));
    }

    // ---------- DELETE /books/{id} ----------

    @Test
    void shouldReturn204WhenDeletingExistingBook() throws Exception {

        //act and assert
        mockMvc.perform(delete("/books/{id}", 1L).with(admin()))
                .andExpect(status().isNoContent());

        verify(bookService).delete(1L);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentBook() throws Exception {

        //arrange
        doThrow(new ResourceNotFoundException(99L)).when(bookService).delete(99L);

        //act and assert
        mockMvc.perform(delete("/books/{id}", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books/99"));

        verify(bookService).delete(99L);
    }

    // ---------- PATCH /books/{id}/copies/add ----------

    @Test
    void shouldReturn200WhenAddingBookCopies() throws Exception {

        //arrange
        when(bookService.addBookCopies(eq(1L), any(BookCopiesDTO.class)))
                .thenReturn(bookResponse(1L, "Dom Casmurro", 15));

        String json = """
                {
                    "quantityCopies": 5
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalCopies").value(15));

        ArgumentCaptor<BookCopiesDTO> captor = ArgumentCaptor.forClass(BookCopiesDTO.class);
        verify(bookService).addBookCopies(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookCopiesDTO(5));
    }

    @Test
    void shouldReturn404WhenAddingCopiesToNonExistentBook() throws Exception {

        //arrange
        when(bookService.addBookCopies(eq(99L), any(BookCopiesDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        String json = """
                {
                    "quantityCopies": 5
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 99L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books/99/copies/add"));

        verify(bookService).addBookCopies(eq(99L), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn422WhenAddingCopiesWithNullQuantity() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": null
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("quantityCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity copies is required"));

        verify(bookService, never()).addBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn422WhenAddingCopiesWithZeroQuantity() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": 0
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("quantityCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid number"));

        verify(bookService, never()).addBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn422WhenAddingCopiesWithNegativeQuantity() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": -3
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("quantityCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid number"));

        verify(bookService, never()).addBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    // ---------- PATCH /books/{id}/copies/remove ----------

    @Test
    void shouldReturn200WhenRemovingBookCopies() throws Exception {

        //arrange
        when(bookService.removeBookCopies(eq(1L), any(BookCopiesDTO.class)))
                .thenReturn(bookResponse(1L, "Dom Casmurro", 6));

        String json = """
                {
                    "quantityCopies": 4
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalCopies").value(6));

        ArgumentCaptor<BookCopiesDTO> captor = ArgumentCaptor.forClass(BookCopiesDTO.class);
        verify(bookService).removeBookCopies(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookCopiesDTO(4));
    }

    @Test
    void shouldReturn409WhenRemovingMoreCopiesThanInStock() throws Exception {

        //arrange
        String message = "It is not possible to remove a quantity greater than what is currently in stock";
        when(bookService.removeBookCopies(eq(1L), any(BookCopiesDTO.class)))
                .thenThrow(new InvalidWithdrawalAmountException(message));

        String json = """
                {
                    "quantityCopies": 11
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Invalid withdrawal amount"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/books/1/copies/remove"));

        verify(bookService).removeBookCopies(eq(1L), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn404WhenRemovingCopiesFromNonExistentBook() throws Exception {

        //arrange
        when(bookService.removeBookCopies(eq(99L), any(BookCopiesDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        String json = """
                {
                    "quantityCopies": 1
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 99L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/books/99/copies/remove"));

        verify(bookService).removeBookCopies(eq(99L), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn422WhenRemovingCopiesWithNullQuantity() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": null
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("quantityCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Quantity copies is required"));

        verify(bookService, never()).removeBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn422WhenRemovingCopiesWithZeroQuantity() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": 0
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 1L).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("quantityCopies"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid number"));

        verify(bookService, never()).removeBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    // ---------- SECURITY ----------

    @Test
    void shouldReturn401WhenRequestHasNoToken() throws Exception {

        //act and assert
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    @Test
    void shouldReturn200WhenUserListsBooks() throws Exception {

        //arrange
        when(bookService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookResponse(1L, "Dom Casmurro", 10)), PageRequest.of(0, 20), 1));

        //act and assert
        mockMvc.perform(get("/books").with(user()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200WhenUserFindsBookById() throws Exception {

        //arrange
        when(bookService.findById(1L)).thenReturn(bookResponse(1L, "Dom Casmurro", 10));

        //act and assert
        mockMvc.perform(get("/books/{id}", 1L).with(user()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserTriesToInsertBook() throws Exception {

        //act and assert
        mockMvc.perform(post("/books").with(user()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(bookService, never()).insert(any(BookRequestDTO.class));
    }

    @Test
    void shouldReturn403WhenUserTriesToUpdateBook() throws Exception {

        //act and assert
        mockMvc.perform(put("/books/{id}", 1L).with(user()).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE_JSON))
                .andExpect(status().isForbidden());

        verify(bookService, never()).update(anyLong(), any(BookUpdateDTO.class));
    }

    @Test
    void shouldReturn403WhenUserTriesToDeleteBook() throws Exception {

        //act and assert
        mockMvc.perform(delete("/books/{id}", 1L).with(user()))
                .andExpect(status().isForbidden());

        verify(bookService, never()).delete(anyLong());
    }

    @Test
    void shouldReturn403WhenUserTriesToAddBookCopies() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": 5
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/add", 1L).with(user()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());

        verify(bookService, never()).addBookCopies(anyLong(), any(BookCopiesDTO.class));
    }

    @Test
    void shouldReturn403WhenUserTriesToRemoveBookCopies() throws Exception {

        //arrange
        String json = """
                {
                    "quantityCopies": 5
                }
                """;

        //act and assert
        mockMvc.perform(patch("/books/{id}/copies/remove", 1L).with(user()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());

        verify(bookService, never()).removeBookCopies(anyLong(), any(BookCopiesDTO.class));
    }
}
